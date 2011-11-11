(ns binpv.test.core
  (:use [binpv.core])
  (:use [clojure.test]))

(defn get-file [] (str "/tmp/foo" (.toString (java.util.UUID/randomUUID)) ".txt"))


; create valid binary file for our protocol
; define our binary protocol
; parse binary file
; visualize binary file

(defn get-bytes [n]
    (take n (map byte (cycle (range -127 128)))))

(defn write-bytes-to-file [the-file the-bytes]
    (spit the-file (String. (byte-array the-bytes))))

; if we can't write bytes and read them back again... uh oh
(deftest write-bytes-and-read-bytes
    (let [test-file (get-file) number-bytes 1000 the-bytes (get-bytes number-bytes)]
        (write-bytes-to-file test-file the-bytes)
        (let [q (slurp test-file)]
            (is true (= (alength (.getBytes q)) number-bytes))
            (is true (= (.getBytes q) (byte-array the-bytes))))))

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

(deftest parse-binary-file 
    (let [test-file (get-file) number-bytes 2000]
        (write-bytes-to-file test-file (get-bytes number-bytes))
        (def parsed (parse-binary test-file key-token-format))
        (is false (nil? parsed))))

; repeat, but for invalid binary file

