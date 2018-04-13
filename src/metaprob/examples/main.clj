;; The file from which this one was derived is ../main.clj

(ns metaprob.examples.main
  (:gen-class)
  (:require [metaprob.examples.inference-on-gaussian :as ginf]
            [criterium.core :as crit]
            ))

(def s-to-ns (* 1000 1000 1000)) ; in ns

(defn instrument [fun & args]
  (flush)
  (if true
    (apply fun args)
    (crit/report-result
     (crit/benchmark*
      fun
      {:warmup-jit-period 0
       :samples 1
       :target-execution-time (* 10 s-to-ns)
       :overhead 0
       }))))

;; For a more serious test, try 100 (takes about an hour?)
(def number-of-samples 5)

(defn -main [& args]
  (print (format "args=%s\n" args))
  (letfn [(combine [arg dict]
            (case arg
              "rejection" (assoc dict :rejection true :any true)
              "importance" (assoc dict :importance true :any true)
              "mh" (assoc dict :mh true :any true)
              (let [matches (re-seq #"^\d+$" arg)]
                (if matches
                  (assoc dict :count (Integer. (first matches)))
                  (assert false ["bad arg" arg])))))
          (reduc [args]    ; I'm sure there's a cool way to do this but it's too
                                        ; hard to slog through the clojure docs
            (if (empty? args)
              {}
              (combine (first args)
                       (reduc (rest args)))))]
    (let [dict (reduc args)
          number-of-samples (or (get dict :count) 5)
          all? (not (get dict :any))]
      (print (format "dict=%s n=%s all=%s\n" dict number-of-samples all?))

      (print "---- Prior ----\n")
      (instrument ginf/gaussian-prior-samples number-of-samples)

      (when (or all? (get dict :rejection))
        ;; Rejection sampling is very slow - 20 seconds per
        (print "---- Rejection ----\n")
        (instrument ginf/rejection-assay number-of-samples))

      (when (or all? (get dict :importance))
        ;; Importance sampling is very fast
        (print "---- Importance ----\n")
        (ginf/importance-assay number-of-samples))

      (when (or all? (get dict :mh))
        ;; MH is fast
        (print "---- MH ----\n")
        (instrument ginf/MH-assay number-of-samples)))))

