(ns pages.index
  (:require
   [cljm.render :refer [page]]
   [babashka.fs :as fs]))

(def title "Your Page Title")

(def main page)

(spit "tmp/index.html" (main))