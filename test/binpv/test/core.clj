(ns binpv.test.core
  (:use [binpv.core])
  (:import [binpv.core ByteBasedChunker FixedLength EnumeratedValue DependentValue FixedLength DependentFixedLength VariableLength FileStreamWrapper])
  (:use [clojure.test]))

(defn get-file [] (str "/tmp/foo" (.toString (java.util.UUID/randomUUID)) ".txt"))

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

; create valid binary file for our protocol
; define our binary protocol
; parse binary file
; visualize binary file

(deftype IncludesPrivateKey
    []
    AnEnumeration
    (get-value [this match-seq]
		(do
			(println "empty?" (nil? match-seq))
			(println "match-seq is " (class  match-seq))
			(println "looking at" (first match-seq))
            ; (println "as char" (first (Character/toChars (first match-seq))))
        (let [first-match (first match-seq)]
            (if (nil? first-match)
                :eof
                (case (first (Character/toChars (first match-seq)))
                    \v :private
                    \b :public
        			:unknown)))))
)

(deftype PrivateKeyPresent
    []
    DependentFun
    (criteria-met? [this sections]
        (= (first (filter :INCLUDES_PRIVATE_KEY sections)) [1]))) ; todo hard coded one value?

(deftype PublicKeyLength
    []
    DependentLength
    (get-length [this parsed-sections]
		(first (:RESULT (first (filter :PUBLIC_KEY_LENGTH parsed-sections)))))) ; todo this ignores the second byte...

(deftype StopAt
    [seq-to-match]
    StopWhen
    (match-found? [this current context sections]
        (let [so-far (conj context current) looking-for (count seq-to-match) right-now (take-last looking-for so-far) done (or (= -1 current) (nil? current))]
            (prn "current is" current)
            (prn "comparing" seq-to-match right-now)
            (if (= seq-to-match right-now)
                {:match true, :new-context right-now :eof done}
                {:match false, :new-context so-far :eof done}))))

(deftype AllDone
    []
    AnEnumeration
    (get-value [this match-seq]
        (= (str match-seq) "AD")))    

(def key-token-format (binary-protocol (ByteBasedChunker.)
    (section :KEY_TOKEN_ID,         (FixedLength. 2))
    (section :INCLUDES_PRIVATE_KEY, (EnumeratedValue. 1 (IncludesPrivateKey.)))
    (section :PRIVATE_KEY,          (DependentValue. 256 (PrivateKeyPresent.)))
    (section :PUBLIC_KEY_LENGTH,    (FixedLength. 2))
    (section :PUBLIC_KEY,           (DependentFixedLength. (PublicKeyLength.)))
    (section :THROW_AWAY,           (VariableLength. (StopAt. [\F \G])))
    (section :THE_END,              (EnumeratedValue. 2 (AllDone.)))))

(deftest parse-binary-file 
    (let [test-file (get-file) number-bytes 2000]
        (write-bytes-to-file test-file (get-bytes number-bytes))
        (def parsed (parse-binary (FileStreamWrapper. test-file) key-token-format))
		(println "parsed is" parsed)
        (is false (nil? parsed))))

; repeat, but for invalid binary file

