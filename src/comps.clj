(ns comps
  (:require [components.counter :refer [main] :rename {main counter-main}]))

(def counter counter-main)
