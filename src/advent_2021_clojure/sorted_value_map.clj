(ns advent-2021-clojure.sorted-value-map
  (:refer-clojure :exclude [assoc merge-with]
                  :rename {dissoc c-dissoc, first c-first}))

(def empty-map {:key-to-value {}, :value-to-keys (sorted-map)})
(defn- value-of [svm k] (get-in svm [:key-to-value k]))
(defn- keys-of [svm v] (get-in svm [:value-to-keys v]))

(defn dissoc [svm k]
  (if-some [old-value (value-of svm k)]
    (if (= #{k} (keys-of svm old-value))
      (-> svm
          (update :key-to-value c-dissoc k)
          (update :value-to-keys c-dissoc old-value))
      (-> svm
          (update :key-to-value c-dissoc k)
          (update-in [:value-to-keys old-value] disj k)))
    svm))

(defn assoc [svm k v]
  (if (= v (value-of svm k))
    svm
    (-> svm
        (dissoc k)
        (assoc-in [:key-to-value k] v)
        (update-in [:value-to-keys v] #(if % (conj % k) (sorted-set k))))))

(defn first [svm]
  (when-some [[v ks] (-> svm :value-to-keys c-first)]
    [(c-first ks) v]))

(defn merge-with [f svm other-map]
  (reduce-kv (fn [acc k v] (if-some [curr-v (value-of svm k)]
                             (if (= curr-v (f v curr-v))
                               acc
                               (assoc acc k v))
                             (assoc acc k v)))
             svm
             other-map))