(ns pages.index
  (:require
   [cljm.render :as rr]
   [hickory.core :as hc]
   [babashka.fs :as fs]))

(def title "Your Page Title")

(def main (fn [] (-> 'pages.index
                     rr/find-template
                     rr/substitute-vars
                     hc/parse
                     hc/as-hiccup
                     rr/resolve-comps
                     rr/render-html
                     str)))

(spit "tmp/index.html" (main))