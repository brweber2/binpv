(ns binpv.core
	(:import [java.io FileInputStream BufferedInputStream]))

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
    (get-section [this stream-seq parsed-so-far]
        (loop [match? (match-found? stop-fun (first stream-seq) [] parsed-so-far) the-rest (rest stream-seq) acc (:new-context match?)]
            (if (:match match?)
                acc
                (recur (match-found? stop-fun (first the-rest) acc parsed-so-far) the-rest [])))))

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


(defn section
	[kw-id section-info]
	{:ID kw-id :SECTION_INFO section-info})

(defn parse-section [chunkable chunker parsed-so-far section]
	{(:ID section) :ID, :RESULT (get-section section (chunk-it chunker chunkable) parsed-so-far)})

(defn binary-protocol [chunker & sections]
	{:BASIS chunker :SECTIONS sections})

(defn parse-binary 
	"todo we need a reader based on the basis..."
	[chunkable binary-protocol]
		(let [basis (:BASIS binary-protocol) to-chunk (get-chunkable-stream chunkable basis) sections (:SECTIONS binary-protocol)]
			(loop [rest-sections sections acc []]
				(if (seq rest-sections)
					(recur (rest sections) (conj acc (parse-section to-chunk basis acc (first sections))))
					acc))))

(defn visualize-section [section]
	)

(defn visualize-binary [parsed-binary]
	)

