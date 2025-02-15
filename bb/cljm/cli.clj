#!/usr/bin/env bb
(ns cljm.cli
  (:require [clojure.string :as s]
            [clojure.core :as c]
            [clojure.zip :as zip]
            [babashka.fs :as fs]
            [hickory.core :as hc]
            [hickory.utils :as hu]
            [hickory.convert :refer [hickory-to-hiccup]]
            [hickory.zip :as hz]
            [huff2.core :as h2]
            [huff2.extension :as h2e]
            [clojure.tools.reader :as r]))


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
                                       (let [result (eval (r/read-string code))]
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

(defn- resolve-var [avar]
  [(keyword (symbol avar)) @avar])

(defn- resolve-clml-component [path]
  "<div>TODO: Component</div>")

(defn- process-clml-file [path]
  (let [clml-content (slurp (fs/file path))
        sss (r/read clml-content)
        [cljc-lines html-lines] (process-clml-content clml-content)
        cljc (s/join "\n" cljc-lines)
        html (s/join "\n" html-lines)
        page-ns (make-ns path)
        current-ns *ns*]
    (in-ns (symbol page-ns))
    (let [vars
          (filter #(var? %) (eval (r/read-string (str "[ " cljc "]"))))
          page-env
          (into {} (map resolve-var vars))]
      (let [html (substitute-vars html)
            hickory (hc/as-hickory (hc/parse html))
            zipper (hz/hickory-zip hickory)
            edited (edit-nodes (fn [node] (and (= :element (:type node)) (hu/starts-with (name (:tag node)) "cl:")))
                               (fn [node] (let [tag (:tag node)
                                                attrs (:attrs node)
                                                component-name (keyword page-ns (s/join (drop 3 (s/split (name tag) #""))))
                                                component-val (component-name page-env)
                                                fn-comp (fn? component-val)
                                                component-html (if fn-comp
                                                                 (component-val attrs)
                                                                 (resolve-clml-component component-val))
                                                node (hc/as-hickory (first (hc/parse-fragment component-html)))]
                                            node))
                               zipper)
            hiccup (hickory-to-hiccup edited)
            html (h2/page (h2/html (nth hiccup 2)))]
        (in-ns current-ns)
        html))))

    (defn -main [in-dir out-dir]
      (let [paths (fs/glob (fs/path in-dir "pages") "**.clml")]
        (doseq [path paths]
          (let [html (process-clml-file path)
                file-name (str (fs/strip-ext (fs/file-name path)) ".html")
                html-path (fs/path out-dir file-name)]
            (fs/create-dirs out-dir)
            (spit (fs/file html-path) html)))))

