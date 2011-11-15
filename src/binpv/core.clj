(ns binpv.core
	(:import [java.io FileInputStream BufferedInputStream])
    (:use binpv.dump))

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

(defn int-to-hex [i]
    (Integer/toHexString i))

(defn int-to-ascii [i]
    (doall (map str (Character/toChars i))))

(defn prhex [s]
    (doall (map int-to-hex s)))

(defn prascii [s]
    (doall (map int-to-ascii s)))

(deftype HexVisualizer
    []
    Visualizer
    (visualize-it [this parsed-section]
        (println parsed-section)
        (if (seq? (:RESULT parsed-section))
            (dump-buffer (:RESULT parsed-section)))
        (println)))
    
(deftype ByteBasedChunker
    []
    Chunker
    (chunk-it [this chunkable]
        (repeatedly #(.read chunkable))))

(deftype FileStreamWrapper
	[source]
	StreamWrapper
	(get-chunkable-stream [this chunker-type]
		(BufferedInputStream. (FileInputStream. source))))

(deftype VariableLength
    [stop-fun]
    SectionInfo
    (get-section [this stream-seq parsed-so-far]
        (loop [ the-rest (rest stream-seq)
                match? (match-found? stop-fun (first stream-seq) [] parsed-so-far) 
                acc (:new-context match?)
                ]
            (when (:eof match?)
                (throw (RuntimeException. "couldn't find end of variable length section")))
            (if (:match match?)
                acc
                (let [new-match (match-found? stop-fun (first the-rest) acc parsed-so-far) ]
                (recur 
                    (rest the-rest)  new-match (:new-context new-match)))))))

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

(defn section
	[kw-id section-info]
	{:ID kw-id :SECTION_INFO section-info})

(defn parse-section [chunkable chunker parsed-so-far section]
	{(:ID section) :ID, :ID (:ID section), :RESULT (get-section (:SECTION_INFO section) (chunk-it chunker chunkable) parsed-so-far)})

(defn binary-protocol [chunker & sections]
	{:BASIS chunker :SECTIONS sections})

(defn parse-binary 
	"todo we need a reader based on the basis..."
	[chunkable binary-protocol]
		(let [basis (:BASIS binary-protocol) to-chunk (get-chunkable-stream chunkable basis) sections (:SECTIONS binary-protocol)]
			(loop [rest-sections sections acc []]
				(if (seq rest-sections)
					(do
					(println "parsing section: " (first rest-sections))
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

