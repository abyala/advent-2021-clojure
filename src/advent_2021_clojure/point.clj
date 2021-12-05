(ns advent-2021-clojure.point)

(defn inclusive-distance [[x1 y1] [x2 y2]]
  (letfn [(local-dist [^long v1 ^long v2] (Math/abs (- v1 v2)))]
    (inc (max (local-dist x1 x2)
              (local-dist y1 y2)))))

(defn inclusive-line-between
  ([[point1 point2]]
   (inclusive-line-between point1 point2))

  ([[x1 y1 :as point1] [x2 y2 :as point2]]
   (letfn [(ordinate-fn [v1 v2] (cond (< v1 v2) +
                                      (> v1 v2) -
                                      :else (fn [v _] v)))]
     (let [distance (inclusive-distance point1 point2)
           x-fn (ordinate-fn x1 x2)
           y-fn (ordinate-fn y1 y2)]
       (map #(vector (x-fn x1 %) (y-fn y1 %)) (range distance))))))

(defn horizontal-line?
  ([[point1 point2]] (horizontal-line? point1 point2))
  ([[_ y1] [_ y2]] (= y1 y2)))

(defn vertical-line?
  ([[point1 point2]] (vertical-line? point1 point2))
  ([[x1 _] [x2 _]] (= x1 x2)))
