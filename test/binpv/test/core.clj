(ns binpv.test.core
  (:use [binpv.core])
  (:use [clojure.test]))

(deftest replace-me 
  (is false "No tests have been written."))


; create valid binary file for our protocol
; define our binary protocol
; parse binary file
; visualize binary file

; helper funs that go into code

(defn get-section [sections id]
    (take 1 (get-sections sections id)))

(defn get-sections [sections id]
    (filter id sections))

; protocol specific helper funs
(defn includes-private-key? [a-byte]
    (case a-byte
        \v :private
        \b :public))

(defn private-key-present? [sections]
    (= (get-section sections :INCLUDES-PRIVATE-KEY) :private))

(defn public-key-length [sections]
    (Integer/parseInt (get-section sections :PUBLIC-KEY-LENGTH)))

(defn stop-at-fun [next-byte context]
    (= \; next-byte))
    
(defn all-done [some-bytes]
    (= (str some-bytes) "AD"))

(defbinp ^{:BASIS :BYTE} [
    (section :FIXED_LENGTH 2, :DESCRIPTION "Key Token Identifier")
    (section :ID :INCLUDES-PRIVATE-KEY, :FIXED_LENGTH 1, :ENUM_FUN includes-private-key?) 
    (section :FIXED_LENGTH 256, :DEPENDENT_FUN private-key-present?, :DESCRIPTION "Private Key") 
    (section :ID :PUBLIC_KEY_LENGTH, :FIXED_LENGTH 2)
    (section :ID :PUBLIC_KEY, :DEPENDENT_FIXED_LENGTH public-key-length)
    (section :ID :THROW-AWAY, :VARIABLE_LENGTH stop-at-fun)
    (section :ID :THE-END, :FIXED_LENGTH 2, :ENUM_FUN all-done))



; repeat, but for invalid binary file

