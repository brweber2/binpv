(ns binpv.core
  (:import [java.io FileInputStream BufferedInputStream]
           [java.util Date]
           [clojure.lang APersistentMap Associative ILookup])
  (:use binpv.dump))

; PROTOCOLS

(defprotocol Visualizer
  (visualize-it [this parsed-section]))

(defprotocol Chunker
  (chunk-it [this chunkable]))

(defprotocol StreamWrapper
  (get-chunkable-stream [this chunker-type]))

(defprotocol SectionInfo
  (get-section [this stream-seq parsed-so-far]))

(defprotocol AnEnumeration
  (get-value [this match-seq]))

(defprotocol DependentFun
  (criteria-met? [this parsed-sections]))

(defprotocol DependentLength
  (get-length [this parsed-sections]))

(defprotocol StopWhen
  (match-found? [this current context parsed-sections]))

; HELPER FUNS

(defn int-to-ascii [i]
  (doall (map str (Character/toChars i))))

(defn all-zeros [s]
  (every? zero? s))

(defn seq-to-num [s]
  (let [lst (reverse s)]
    (loop [items lst index 0 result 0]
      (if (seq items)
        (recur (rest items) (inc index) (+ result (* (.pow (BigInteger. "256") index) (first items))))
        (long result)))))

; VISUALIZERS

(deftype HexVisualizer
  []
  Visualizer
  (visualize-it [this parsed-section]
    (println (format "%20s:" (name (:ID parsed-section))))
    (let [result (:RESULT parsed-section)]
      (if (seq? result)
        (if (all-zeros result)
          (println (format "%22s%s" "" (str "All " (count result) " bytes are null.")))
          (dump-buffer result))
        (println result)))
    (println)))

(deftype IntegerVisualizer
  []
  Visualizer
  (visualize-it [this parsed-section]
    (println (format "%20s: %s" (name (:ID parsed-section)) (seq-to-num (:RESULT parsed-section))))))

(deftype DateVisualizer
  []
  Visualizer
  (visualize-it [this parsed-section]
    (let [date-as-num (seq-to-num (:RESULT parsed-section))]
      (println (format "%20s: %s [%s]" (name (:ID parsed-section)) date-as-num (.toGMTString (Date. (long (* 100 date-as-num)))))))))

(deftype BinaryProtocolVisualizer
  [visualizers]
  Visualizer
  (visualize-it [this parsed-section]
    (println (format "%20s: {" (name (:ID parsed-section))))
    (doall (map visualize-it visualizers (:RESULT parsed-section)))
    (println (format "%23s" "}"))))

; HACK, LOOK AWAY

; ummm, don't do this
(deftype MutableIterator
  [f]
  java.util.Iterator
  (next [this]
    (f))
  (hasNext [this]
    true)
  (remove [this]
    nil))

; ummm, don't do this
(deftype MutableIterable
  [f]
  Iterable
  (iterator [this]
    (MutableIterator. f)))

; OUR TYPES

(deftype ByteBasedChunker
  []
  Chunker
  (chunk-it [this chunkable]
    (MutableIterable. #(.read chunkable))))

(deftype FileStreamWrapper
  [source]
  StreamWrapper
  (get-chunkable-stream [this chunker-type]
    (BufferedInputStream. (FileInputStream. source))))

(deftype VariableLength
  [stop-fun]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (loop [the-rest (rest stream-seq)
           match? (match-found? stop-fun (first stream-seq) [] parsed-so-far)
           acc (:new-context match?)
           ]
      (when (:eof match?)
        (throw (RuntimeException. "couldn't find end of variable length section")))
      (if (:match match?)
        acc
        (let [new-match (match-found? stop-fun (first the-rest) acc parsed-so-far)]
          (recur
            (rest the-rest) new-match (:new-context new-match)))))))

(deftype BinaryProtocol
  [mp]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (loop [sections (:SECTIONS mp) parsed []]
      (if (seq sections)
        (let [section (first sections) the-rest (rest sections) result (get-section (:SECTION_INFO section) stream-seq parsed) the-list (conj parsed {(:ID section) :ID, :ID (:ID section), :RESULT result})]
          (recur the-rest the-list))
        parsed)))
  Associative
  (containsKey [this _key]
    (.containsKey mp _key))
  (entryAt [this _key]
    (.entryAt mp _key))
  (assoc [this _key _value]
    (.assoc mp _key _value))
  ILookup
  (valAt [this _key]
    (.valAt mp _key))
  (valAt [this _key _notFound]
    (.valAt mp _key _notFound)))

(deftype FixedLength
  [the-length]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (take the-length stream-seq)))

(deftype EnumeratedValue
  [the-length an-enumeration]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (let [the-seq (take the-length stream-seq)]
      (get-value an-enumeration the-seq))))

(deftype DependentValue
  [the-length dependent-fun]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (when (criteria-met? dependent-fun parsed-so-far)
      (take the-length stream-seq))))

(deftype DependentFixedLength
  [dependent-length]
  SectionInfo
  (get-section [this stream-seq parsed-so-far]
    (take (get-length dependent-length parsed-so-far) stream-seq)))

(deftype StopAt
  [seq-to-match]
  StopWhen
  (match-found? [this current context sections]
    (let [so-far (conj context current) looking-for (count seq-to-match) right-now (take-last looking-for so-far) done (or (= -1 current) (nil? current))]
      (if (= (map int seq-to-match) right-now)
        {:match true, :new-context right-now :eof done}
        {:match false, :new-context so-far :eof done}))))

; OUR LOGIC

(defn section
  [kw-id section-info]
  {:ID kw-id :SECTION_INFO section-info})

(defn parse-section [chunkable chunker parsed-so-far section]
  {(:ID section) :ID, :ID (:ID section), :RESULT (get-section (:SECTION_INFO section) (chunk-it chunker chunkable) parsed-so-far)})

(defn binary-protocol [chunker & sections]
  (let [_sections (seq sections)]
    (BinaryProtocol. {:BASIS chunker :SECTIONS _sections})))

(defn parse-binary
  "todo we need a reader based on the basis..."
  [chunkable binary-protocol]
  (let [basis (:BASIS binary-protocol) to-chunk (get-chunkable-stream chunkable basis) sections (:SECTIONS binary-protocol)]
    (loop [rest-sections sections acc []]
      (if (seq rest-sections)
        (do
          ;(println "parsing section: " (first rest-sections))
          (recur (rest rest-sections) (conj acc (parse-section to-chunk basis acc (first rest-sections)))))
        acc))))

(defn visualize-section [visualizer parsed-section]
  (when visualizer
    (visualize-it visualizer parsed-section)))

(defn visualize-binary [visualizers parsed-binary]
  (do
    (println "****************** START BINARY  ****************** ")
    (doall (map visualize-section visualizers parsed-binary))
    (println "****************** END BINARY  ****************** ")))

