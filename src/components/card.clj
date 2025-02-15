(ns components.card
  (:require
   [cljm.render :as rr]
   [hickory.core :as hc]
   [hiccup2.core :as h2]))

(def ^:dynamic slot1)
(def ^:dynamic slot2)

(defn main [_ attrs & children]
  (let [tag-children (filter coll? children)
        template (rr/find-template 'components.card)
        ns *ns*]
    (in-ns 'components.card)
    (let [res (binding [slot1 (str (h2/html (first tag-children)))
                        slot2 (str (h2/html (second tag-children)))]
                (let [filled-in (rr/substitute-vars template)
                      parsed (hc/parse-fragment filled-in)]
                  (map hc/as-hiccup parsed))) ;; Maybe a bug
          resolved (rr/resolve-comps (first res))]
      (in-ns ns)
      resolved)))
