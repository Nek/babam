#!/usr/bin/env bb

 (ns parser.main
   (:require [clojure.string :as s]
             [clojure.core :as c]
             [clojure.zip :as zip]
             [babashka.fs :as fs]
             [hickory.core :as hc]
             [hickory.utils :as hu]
             [hickory.render :refer [hickory-to-html]]
             [hickory.convert :refer [hickory-to-hiccup]]
             [hickory.select :as hs]
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

(defn -main [input]
  (let [current-ns *ns*
        content (c/slurp input)
        [cljc html] (s/split content #"(?m)^---$")
        NS_UID (hash input)
        ns (fs/file-name (first (drop-last (fs/split-ext input))))]
    (c/in-ns (symbol ns))
    (println *ns* ns)
    (c/eval (c/read-string (str "(do " cljc ")")))
    (def comp-qualifier (keyword (str NS_UID "/component")))
    (def my-schema (h2e/add-schema-branch h2/hiccup-schema :index/component))
    (def ns-keys (keys (ns-interns *ns*)))
    (def component-fns (into {} (filter (fn [[_ val]] (c/fn? val)) (map (fn [k] 
                                                                          (let [q-key (keyword (str ns "/" k))]
                                                                          [q-key (eval k)])) ns-keys))))
    (defmethod h2/emit :index/component [append! node opts]
      (let [[_ [_ [attrs & children]]] node
            component-name (get-in attrs [:component])
            component-fn (get component-fns component-name)
            clean-attrs (dissoc attrs :component)]
        (println "component-name" component-name "children" children "opts" opts)
        (append! (component-fn clean-attrs children))))
    (let [html (s/replace html #"\{\{([^}]+)\}\}" (fn [[_ code]]
                                                    (let [result (c/eval (c/read-string code))]
                                                    ;;   (println "code" code "result" result)
                                                      (str result))))
          hickory (hc/as-hickory (hc/parse html))
        ;;   hiccup (hickory-to-hiccup hickory)
          _ (println "hickory" (type hickory))
          zipper (hz/hickory-zip hickory)
          _ (println "zipper" (type zipper))
          edited (edit-nodes (fn [node] (and (= :element (:type node)) (hu/starts-with (name (:tag node)) "c:")))
                             (fn [node] (let [tag (:tag node)
                                              name (name tag)
                                              new-tag (keyword (s/replace name "c:" (str *ns* "/")))
                                              component-name (keyword (s/replace name "c:" (str ns "/")))]
                                        ;;   (println "tag" tag "new-tag" new-tag "component-name" component-name)
                                          (-> node
                                              (assoc :tag new-tag)
                                              (update-in [:attrs] assoc :component component-name))))
                             zipper)]
      
      (def v (hickory-to-hiccup edited))
      (println "v" v)
      (println (h2/html (h2e/custom-fxns! my-schema) v))
    ;;   (println (hickory-to-html edited))
      (c/in-ns current-ns)
      (println *ns*))))
