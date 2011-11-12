(ns binpv.core
	(:import [java.io FileInputStream BufferedInputStream]))

(defprotocol Chunker
    []
    (chunk-it [this chunkable]))

(defprotocol StreamWrapper
	[]
	(get-chunkable-stream [this chunker-type]))

(defprotocol SectionInfo
    (get-section [this stream-seq]))

(defprotocol AnEnumeration
    (get-value [this match-seq]))

(defprotocol DependentFun
    [parsed-sections]
    (criteria-met? [this]))

(defprotocol DependentLength
    []
    (get-length [this parsed-sections]))

(defprotocol StopWhen
    []
    (match-found? [this current context]))

(deftype ByteBasedChunker
    []
    Chunker
    (chunk-it [this chunkable]
    	(repeatedly (.read chunkable))))

(deftype FileStreamWrapper
	[source]
	StreamWrapper
	(get-chunkable-stream [this chunker-type]
		(BufferedInputStream. (FileInputStream. source))))

(deftype VariableLength
    [stop-fun]
    SectionInfo
    (get-section [this stream-seq]
        (loop [match? (match-found? (first stream-seq) []) the-rest (rest stream-seq) acc (:new-context match?)]
            (if (:match match?)
                acc
                (recur (match-found? (first the-rest) acc) the-rest)))))

(deftype FixedLength 
    [the-length]
    SectionInfo
    (get-section [this stream-seq]
        (take the-length stream-seq)))

(deftype EnumeratedValue
    [the-length an-enumeration]
    SectionInfo
    (get-section [this stream-seq]
        (let [the-seq (take the-length stream-seq)]
            (get-value an-enumeration the-seq))))

(deftype DependentValue
    [the-length dependent-fun]
    SectionInfo
    (get-section [this stream-seq]
        (when (criteria-met? dependent-fun)
            (take the-length stream-seq))))

(deftype DependentFixedLength
    [dependent-length]
    SectionInfo
    (get-length [this stream-seq]
        (take (get-length dependent-length) stream-seq)))


(defn section
	[kw-id section-info]
	{:ID kw-id :SECTION_INFO section-info})

(defn parse-section [chunkable chunker section]
	{(:ID section) :ID, :RESULT (get-section (chunk-it chunker chunkable))})

(defn binary-protocol [chunker & sections]
	{:BASIS chunker :SECTIONS sections})

(defn parse-binary 
	"todo we need a reader based on the basis..."
	[chunkable binary-protocol]
		(doall (map (partial parse-section chunkable (:BASIS binary-protocol)) (:SECTIONS binary-protocol))))

(defn visualize-section [section]
	)

(defn visualize-binary [parsed-binary]
	)

