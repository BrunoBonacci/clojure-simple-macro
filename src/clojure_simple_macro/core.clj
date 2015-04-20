(ns clojure-simple-macro.core)

;; syntax-quote ` produces a template
;; of the form which follow the backqouote

;; if we write
(println "hello")
;;> hello        [in stdout]

;; it is executed and produces the output "hello"
;; however if we write:
`(println "hello")
;;=> (clojure.core/println "hello")

;; note the namespace is added automatically
;; this actually produces a template
;; with the enclosed forms.


;; this macro execute the `operation`
;; and if an exception occurs the
;; the `default-value` is returned instead
(defmacro default-to [default-value operation]
  `(try
     ~operation
     (catch Exception x#
       ~default-value)))

;; we use `macroexpand-1` to "apply" the template
;; and see the generated code
(macroexpand-1
 '(default-to 10  (/ 1 4)))

;;=>
;; (try
;;   (/ 1 4)
;;   (catch java.lang.Exception x__7759__auto__
;;     10))

;; Let's try to see how our macro works.
(default-to 10 (/ 1 4))
;;=> 1/4

(default-to 10 (/ 1 0))   ;; Exception
;;=> 10

;; importing a logging library
(require '[taoensso.timbre :as log])

;; now let's enhance our macro to log the error
(defmacro default-to [default-value operation]
  `(try
     ~operation
     (catch Exception x#
       (log/debug "The following error occurred:" x#
                  ", defaulting to:" ~default-value)
       ~default-value)))

;; let's assume that we fetch the default value
;; from a db and we have a commodity function
;; called `load-default-value` which return
;; the default-value
(defn load-default-value []
  (println "loading default value from database")
  (comment loading from db)
  3)

(load-default-value)
;;=> 3

(macroexpand-1
 '(default-to (load-default-value)
     (/ 1 0)))
;;=>
;; (try
;;  (/ 1 0)
;;  (catch  java.lang.Exception x__9554__auto__
;;   (taoensso.timbre/debug "The following error occurred:" x__9554__auto__
;;                          ", defaulting to:" (load-default-value))
;;   (load-default-value)))

;; let's execute the macro
(default-to (load-default-value)
   (/ 1 0))
;; [check your stdout] the load-default-value is execute twice
;;=> 3

;; now let's fix the problem by wrapping with a let form
;; the part of the code which uses the placeholder
;; multiple times
(defmacro default-to [default-value operation]
  `(try
     ~operation
     (catch Exception x#
       (let [default# ~default-value]
         (log/debug "The following error occurred:" x#
                    ", defaulting to:" default#)
         default#))))



(macroexpand-1
 '(default-to (load-default-value)
     (/ 1 0)))

;;=>
;; (try
;;  (/ 1 0)
;;  (catch java.lang.Exception x__6188__auto__
;;   (clojure.core/let [default__6189__auto__ (load-default-value)]
;;    (taoensso.timbre/debug "The following error occurred:" x__6188__auto__
;;                           ", defaulting to:" default__6189__auto__)
;;    default__6189__auto__)))


;; now `load-default-value` is called only once.
(default-to (load-default-value)
   (/ 1 0))


;; finally we can improve our macro to accept
;; multiple forms in the `operation` param
;; we achieve this by adding the variadic parameter
;; in our signature and using unquote-splicing
(defmacro default-to [default-value & operations]
  `(try
     ;; this will expand the sequence `operations`
     ;; with its elements.
     ~@operations
     (catch Exception x#
       (let [default# ~default-value]
         (log/debug "The following error occurred:" x#
                    ", defaulting to:" default#)
         default#))))


;; let's see the macroexpansion
(macroexpand-1
 '(default-to (load-default-value)
    (println "This is a multi sexpr operation")
    (println "Infact it will be captured by &operation as a list")
    (/ 1 0)))

;;=>
;; (try
;;   (println "This is a multi sexpr operation")
;;   (println "Infact it will be captured by &operation as a list")
;;   (/ 1 0)
;;   (catch java.lang.Exception x__8178__auto__
;;     (clojure.core/let [default__8179__auto__ (load-default-value)]
;;       (taoensso.timbre/debug "The following error occurred:" x__8178__auto__
;;                              ", defaulting to:" default__8179__auto__)
;;       default__8179__auto__)))


;; let's understand better how unquote-splicing works
;; range return a sequence of numbers
(range 10)
;;=> (0 1 2 3 4 5 6 7 8 9)

;; if we use the normal unquote
;; number will appear wrapped in a sequence
`(max ~(range 10))    ;; wrong, need (apply max ...)
;;=> (clojure.core/max (0 1 2 3 4 5 6 7 8 9))

;; notice here that the number are NOT wrapped
;; into the sequence but they appear directly
`(max ~@(range 10))
;;=> (clojure.core/max 0 1 2 3 4 5 6 7 8 9)
