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

(defn edit-nodes [condition edit-fn z]
  (loop [loc z]
    (if (zip/end? loc)
      (zip/root loc)
      (let [node (zip/node loc)]
        (if (condition node)
          (recur (zip/edit loc edit-fn)) ;; Edit the node if it satisfies the condition
          (recur (zip/next loc))))))) ;; Move to the next node
 
(defn process-page [path output-dir]
  (let [current-ns *ns*
        content (c/slurp (fs/file path))
        [cljc html] (s/split content #"(?m)^---$")
        NS_UID (str "h-" (hash path))
        NS_COMP_KEYWORD (keyword (str NS_UID "/component"))
        ns (fs/file-name (first (drop-last (fs/split-ext path))))]
    (c/in-ns (symbol ns))
    (c/eval (c/read-string (str "(do " cljc ")")))
    (def my-schema (h2e/add-schema-branch h2/hiccup-schema NS_COMP_KEYWORD))
    (def ns-keys (keys (ns-interns *ns*)))
    (def component-fns (into {} (filter (fn [[_ val]] (c/fn? val)) (map (fn [k]
                                                                          (let [q-key (keyword (str ns "/" k))]
                                                                            [q-key (eval k)])) ns-keys))))
    (defmethod h2/emit NS_COMP_KEYWORD [append! node opts]
      (let [[_ [_ [attrs & children]]] node
            component-name (get-in attrs [NS_COMP_KEYWORD])
            component-fn (get component-fns component-name)
            clean-attrs (dissoc attrs NS_COMP_KEYWORD)]
        (append! (component-fn clean-attrs children))))
    (let [html (s/replace html #"\{\{([^}]+)\}\}" (fn [[_ code]]
                                                    (let [result (c/eval (c/read-string code))]
                                                      (str result))))
          hickory (hc/as-hickory (hc/parse html))
          zipper (hz/hickory-zip hickory)
          edited (edit-nodes (fn [node] (and (= :element (:type node)) (hu/starts-with (name (:tag node)) "c:")))
                             (fn [node] (let [tag (:tag node)
                                              name (name tag)
                                              new-tag  NS_COMP_KEYWORD
                                              component-name (keyword (s/replace name "c:" (str ns "/")))]
                                          (-> node
                                              (assoc :tag new-tag)
                                              (update-in [:attrs] assoc NS_COMP_KEYWORD component-name))))
                             zipper)
          hiccup (hickory-to-hiccup edited)]
      (println hiccup)
      (println (h2/page (h2/html (h2e/custom-fxns! my-schema) (nth hiccup 2))))
      (fs/create-dirs output-dir)
      (c/spit (str output-dir "/" ns ".html") (h2/page (h2/html (h2e/custom-fxns! my-schema) (nth hiccup 2))))
      (c/in-ns current-ns))))

(defn -main [input output]
  (let [files (fs/glob input "**.clml")]
    (println "Processing" files)
    (doseq [file files]
      (process-page file output))))
