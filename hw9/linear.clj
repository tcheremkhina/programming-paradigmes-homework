(defn projector [i] (fn [args] {
                                :pre  [(< 0 (count args)) (<= 0 i) (< i (count (nth args 0)))]
                                :post [(= (count %) (count args))]
                                } (mapv (fn [j] (nth j i)) args)))

(defn checkForEvery [f] (fn [args] {
                                    :pre [(or (= (type args) clojure.lang.PersistentVector)
                                              (seq? args))]
                                    } ((defn check [i]
                                             (cond
                                               (= i (count args)) true
                                               :else (and (f (nth args i) (first args)) (recur (inc i))))) 0)))

(def checkLength (checkForEvery (fn [x y] (= (count x) (count y)))))
(def checkNumbers (checkForEvery (fn [x & y] (isa? (type x) Number))))
(def checkVectors (checkForEvery (fn [x & y] (and (= (type x) clojure.lang.PersistentVector) (checkNumbers x)))))
(def checkMatrix (checkForEvery (fn [x & y] (and (checkVectors x) (checkLength x)))))

(defn toVector [args] (mapv identity args))

(defn vf [f] (fn [& args] {
                           :pre  [(checkVectors args) (checkLength args)]
                           :post [(= (count (nth args 0)) (count %))]
                           } (mapv (fn [i] (apply f ((projector i) args))) (range (count (nth args 0))))))
(def v+ (vf +))
(def v- (vf -))
(def v* (vf *))

(defn mf [f] (fn [& args] {
                           :pre  [(checkMatrix args) (checkLength args)]
                           :post [(= (count (nth args 0)) (count %)) (checkLength %)]
                           } (mapv (fn [i] (apply (vf f) ((projector i) args))) (range (count (nth args 0))))))
(def m+ (mf +))
(def m- (mf -))
(def m* (mf *))

(defn v*s [v & s] {
                   :pre  [(checkVectors [v]) (or (= (count s) 0) (checkNumbers s))]
                   :post [(= (count v) (count %))]
                   } (mapv (fn [i] (* i (apply * s))) v))


(defn m*s [m & s] {
                   :pre  [(< 0 (count m)) (checkMatrix [m]) (or (= (count s) 0) (checkNumbers s))]
                   :post [(= (count m) (count %))]
                   } (mapv (fn [i] (v*s i (apply * s))) m))

(defn m*v [m v] {
                 :pre  [(checkMatrix [m]) (checkVectors [v]) (= (count (nth m 0)) (count v))]
                 :post [(= (count %) (count m))]
                 } (mapv (fn [i] (apply + (v* (nth m i) v))) (range (count m))))

(defn m*m [m1 & args] {
                       :pre  [(checkMatrix [m1])
                              (or (= (count args) 0) (println (first args))
                                  (and (checkMatrix [(first args)])
                                       (= (count (first args)) (count (nth m1 0)))))]
                       :post [(checkMatrix [%])
                              (or (and (= (count args) 0) (= % m1))
                                  (and (= (count (nth (last args) 0))
                                          (count (nth % 0))) (= (count %) (count m1))))]
                       } (cond
                           (= (count args) 0) m1
                           (= (count args) 1) (mapv
                                                (fn [i] (mapv
                                                          (fn [j] (apply + (v* ((projector j) (first args)) (nth m1 i))))
                                                          (range (count (nth (nth args 0) 0))))) (range (count m1)))
                           :else (apply m*m (m*m m1 (first args)) (subvec (toVector args) 1))))

(defn transpose [m] {
                     :pre  [(checkMatrix [m])]
                     :post [(checkMatrix [%]) (= (count (first m)) (count %)) (= (count m) (count (first %)))]
                     } (mapv (fn [i] ((projector i) m)) (range (count (nth m 0)))))

(defn det [args a b] {
                      :pre [(checkMatrix [args]) (= (count args) 2) (= (count (first args)) 3)]
                      } (- (* (nth (nth args 0) a) (nth (nth args 1) b))
                           (* (nth (nth args 0) b) (nth (nth args 1) a))))

(defn vect [& args] {
                     :pre [(checkMatrix [args]) (= (count (nth args 0)) 3)]
                     } (cond
                         (= (count args) 3) (vect (vect (nth args 0) (nth args 1)) (nth args 2))
                         (= (count args) 2) (vector (det args 1 2) (- (det args 0 2)) (det args 0 1))
                         (= (count args) 1) (nth args 0)
                         :else [0 0 0]))
(defn scalar [& args] {
                       :pre [(checkMatrix [args])]
                       } (apply + (apply v* args)))