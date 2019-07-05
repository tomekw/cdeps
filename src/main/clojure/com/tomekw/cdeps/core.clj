(ns com.tomekw.cdeps.core
  (:gen-class)
  (:require [com.tomekw.cdeps.calculator :refer :all]))

(defn -main [& args]
  (do (println (format "2 + 2 is %s" (plus 2 2)))
      (println (format "4 / 2 is %s" (divide 4 2)))))
