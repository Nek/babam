#!/usr/bin/env bb
(ns parser.main
  (:require [clojure.string :as s]
            [clojure.core :as c]
            [clojure.zip :as zip]
            [babashka.fs :as fs]
            [hickory.core :as hc]
            [hickory.utils :as hu]
            [hickory.convert :refer [hickory-to-hiccup]]
            [hickory.zip :as hz]
            [huff2.core :as h2]
            [huff2.extension :as h2e]))

(defn- edit-nodes [condition edit-fn z]
  (loop [loc z]
    (if (zip/end? loc)
      (zip/root loc)
      (let [node (zip/node loc)]
        (if (condition node)
          (recur (zip/edit loc edit-fn)) ;; Edit the node if it satisfies the condition
          (recur (zip/next loc))))))) ;; Move to the next node

(defn- substitute-vars [html]
  (s/replace html #"\{\{([^}]+)\}\}" (fn [[_ code]]
                                       (let [result (eval (read-string code))]
                                         (str result)))))


(defn- not=sep [line] (not= line "#_(-->"))


(defn- split-and-clean [lines] (let [cljc-lines (take-while not=sep lines)
                                     html-lines (reverse (take-while not=sep (reverse lines)))]
                                 [cljc-lines html-lines]))

(defn- process-clml-content [content]
  (-> content
      s/split-lines
      rest
      drop-last
      split-and-clean))

(defn- make-ns [path]
  (fs/file-name (first (drop-last (fs/split-ext path)))))

(defn- process-clml-page [path clml-content output-dir]
  (let [current-ns *ns*
        [cljc-lines html-lines] (process-clml-content clml-content)
        cljc (s/join "\n" cljc-lines)
        html (s/join "\n" html-lines)
        NS_UID (str "h-" (hash path))
        NS_COMP_KEYWORD (keyword (str NS_UID "/component"))
        NS (make-ns path)]
    (in-ns (symbol NS))
    (eval (read-string (str "(do " cljc ")")))
    (def my-schema (h2e/add-schema-branch h2/hiccup-schema NS_COMP_KEYWORD))
    (def ns-keys (keys (ns-interns *ns*)))
    (def component-fns (into {} (filter (fn [[_ val]] (fn? val)) (map (fn [k]
                                                                        (let [q-key (keyword (str NS "/" k))]
                                                                          [q-key (eval k)])) ns-keys))))
    (defmethod h2/emit NS_COMP_KEYWORD [append! node _]
      (let [[_ [_ [attrs & children]]] node
            component-name (get-in attrs [NS_COMP_KEYWORD])
            component-fn (get component-fns component-name)
            clean-attrs (dissoc attrs NS_COMP_KEYWORD)]
        (append! (component-fn clean-attrs children))))
    (let [html (substitute-vars html)
          hickory (hc/as-hickory (hc/parse html))
          zipper (hz/hickory-zip hickory)
          edited (edit-nodes (fn [node] (and (= :element (:type node)) (hu/starts-with (name (:tag node)) "c:")))
                             (fn [node] (let [tag (:tag node)
                                              name (name tag)
                                              new-tag  NS_COMP_KEYWORD
                                              component-name (keyword (s/replace name "c:" (str NS "/")))]
                                          (-> node
                                              (assoc :tag new-tag)
                                              (update-in [:attrs] assoc NS_COMP_KEYWORD component-name))))
                             zipper)
          hiccup (hickory-to-hiccup edited)
          html (h2/page (h2/html (h2e/custom-fxns! my-schema) (nth hiccup 2)))]
      (in-ns current-ns)
      html)))

(defn- process-clml-component [path clml-content out-dir] "<div>TODO: Components</div>")

(defn -main [in-dir out-dir]
  (let [paths (fs/glob in-dir "**.clml")]
    (doseq [path paths]
      (let [clml-content (slurp (fs/file path))
            is-page (s/includes? clml-content "<!DOCTYPE html>")
            html (if is-page
                   (process-clml-page path clml-content out-dir)
                   (process-clml-component path clml-content out-dir))
            file-name (str (fs/strip-ext (fs/file-name path)) ".html")
            html-path (fs/path out-dir file-name)]
        (fs/create-dirs out-dir)
        (spit (fs/file html-path) html)))))
