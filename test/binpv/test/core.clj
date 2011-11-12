(ns binpv.test.core
  (:use [binpv.core])
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
        (case (first (Character/toChars (first the-bytes)))
            \v :private
            \b :public
            :else (throw (RuntimeException.)))))

(deftype PrivateKeyPresent
    []
    DependentFun
    (criteria-met? [this sections]
        (= (first (filter :INCLUDES_PRIVATE_KEY sections)) [1])))

(deftype PublicKeyLength
    []
    DependentLength
    (get-length [this parsed-sections]
        (Integer/parseInt (first (filter :PUBLIC_KEY_LENGTH sections)))))

(deftype StopAt
    [seq-to-match]
    StopWhen
    (match-found? [this current context]
        (let [so-far (conj current context) looking-for (count seq-to-match) right-now (take-last looking-for so-far)]
            (if (= seq-to-match right-now)
                {:match true, :new-context right-now}
                {:match false, :new-context so-far}))))

(deftype AllDone
    []
    AnEnumeration
    (get-value [this match-seq]
        (= (str some-bytes) "AD"))    

(def key-token-format (binary-protocol (ByteBasedChunker.)
    (section :KEY_TOKEN_ID,         (FixedLength. 2))
    (section :INCLUDES_PRIVATE_KEY, (EnumeratedValue. 1 (IncludesPrivateKey.)))
    (section :PRIVATE_KEY,          (DependentValue. 256 (PrivateKeyPresent.)))
    (section :PUBLIC_KEY_LENGTH,    (FixedLength. 2))
    (section :PUBLIC_KEY,           (DependentFixedLength. (PublicKeyLength.)))
    (section :THROW_AWAY,           (VariableLength. (StopAt. [\space\n])))
    (section :THE_END,              (EnumeratedValue. 2 (AllDone.)))))

(deftest parse-binary-file 
    (let [test-file (get-file) number-bytes 2000]
        (write-bytes-to-file test-file (get-bytes number-bytes))
        (def parsed (parse-binary (FileStreamWrapper. test-file) key-token-format))
        (prn parsed)
        (is false (nil? parsed))))

; repeat, but for invalid binary file

