(ns advent-2021-clojure.sorted-value-map
  (:refer-clojure :exclude [assoc dissoc first merge merge-with])
  (:require [clojure.core :as core]))

(def empty-map {:key-to-value {}, :value-to-keys (sorted-map)})
(defn create [] empty-map)

(defn dissoc [svm k]
  (if-some [old-value (get-in svm [:key-to-value k])]
    (if (= #{k} (get-in svm [:value-to-keys old-value]))
      (-> svm
          (update :key-to-value core/dissoc k)
          (update :value-to-keys core/dissoc old-value))
      (-> svm
          (update :key-to-value core/dissoc k)
          (update-in [:value-to-keys old-value] disj k)))
    svm))

(defn assoc [svm k v]
  (let [old-value (get-in svm [:key-to-value k])
        working-svm (if old-value (dissoc svm k) svm)]
    (-> working-svm
        (assoc-in [:key-to-value k] v)
        (update-in [:value-to-keys v] #(if % (conj % k) (sorted-set k))))))

(defn first [svm]
  (when-some [[v ks] (-> svm :value-to-keys core/first)]
    [(core/first ks) v]))

(defn merge [svm other-map]
  (reduce-kv #(assoc %1 %2 %3) svm other-map))

(defn merge-with [f svm other-map]
  (reduce-kv (fn [acc k v]
               (if-some [curr-v (get-in acc [:key-to-value k])]
                 (if (= v (f v curr-v))
                   (assoc acc k v)
                   acc)
                 (assoc acc k v)))
             svm
             other-map))