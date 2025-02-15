(ns my-test
  (:require [clojure.test :refer [deftest is testing]]
            [cljm.render :as render]))

(deftest my-first-test
  (testing "equality works"
    (is (= 1 1))))

(deftest my-second-test
  (testing "equality still works"
    (is (= 2 2))))

(deftest my-third-test
  (testing "equality still works 2"
    (is (= 3 3))))