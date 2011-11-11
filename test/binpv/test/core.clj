(ns binpv.test.core
  (:use [binpv.core])
  (:use [clojure.test]))

(def THE_FILE "/tmp/foo.txt")


; create valid binary file for our protocol
; define our binary protocol
; parse binary file
; visualize binary file

(defn get-bytes [n]
    (take n (map byte (cycle (range -127 128)))))

(deftest foo
        (spit THE_FILE (String. (byte-array (get-bytes 1000))))
        (def q (slurp THE_FILE))
        (is true (= (alength (.getBytes q)) 1000))
        )



; protocol specific helper funs
(defn includes-private-key? [the-bytes]
    ; todo add precondition that the size of the-bytes is one?
    (case (first (Character/toChars (first the-bytes)))
        \v :private
        \b :public))

(defn private-key-present? [sections]
    (= (get-section sections :INCLUDES_PRIVATE_KEY) :private))

(defn public-key-length [sections]
    (Integer/parseInt (get-section sections :PUBLIC_KEY_LENGTH)))

(defn stop-at-fun [next-byte context]
    [(= \; next-byte) context])
    
(defn all-done [some-bytes]
    (= (str some-bytes) "AD"))

(defbinp key-token-format :BASIS :BYTE
    (section :ID :KEY_TOKEN_ID,         :FIXED_LENGTH 2, :DESCRIPTION "Key Token Identifier")
    (section :ID :INCLUDES_PRIVATE_KEY, :FIXED_LENGTH 1, :ENUM_FUN includes-private-key?) 
    (section :ID :PRIVATE_KEY,          :FIXED_LENGTH 256, :DEPENDENT_FUN private-key-present?) 
    (section :ID :PUBLIC_KEY_LENGTH,    :FIXED_LENGTH 2)
    (section :ID :PUBLIC_KEY,           :DEPENDENT_FIXED_LENGTH public-key-length)
    (section :ID :THROW_AWAY,           :VARIABLE_LENGTH stop-at-fun)
    (section :ID :THE_END,              :FIXED_LENGTH 2, :ENUM_FUN all-done))


; repeat, but for invalid binary file

