(ns binpv.core
	(:import [java.io FileInputStream BufferedInputStream]))

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
        (print (:ID parsed-section))
        (print (prhex (:RESULT parsed-section)))
        (print " : ")
        (print (prascii (:RESULT parsed-section)))
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
		; (do
			(prn "in variable length")
			(prn "stream seq" (class stream-seq))
			(prn "match? " (match-found? stop-fun (first stream-seq) [] parsed-so-far))
			(prn "the rest" (class stream-seq))
			(prn "new ctx" (:new-context (match-found? stop-fun (first stream-seq) [] parsed-so-far)))
        (loop [ the-rest (rest stream-seq)
                match? (match-found? stop-fun (first stream-seq) [] parsed-so-far) 
                acc (:new-context match?)
                ]
						 (prn "match?" match?)
						 (prn "the-rest" (class the-rest))
						 (prn "acc" acc)
            (when (:eof match?)
                (throw (RuntimeException. "couldn't find end of variable length section")))
            (if (:match match?)
                acc
                (let [new-match (match-found? stop-fun (first the-rest) acc parsed-so-far) ]
                (recur 
                    (rest the-rest) 
                    new-match
                    (:new-context new-match)))))))
		; )

(deftype FixedLength 
    [the-length]
    SectionInfo
    (get-section [this stream-seq parsed-so-far]
        (do 
            (prn "wtf stream seq" (class stream-seq))
            (prn "the-length" the-length)
		(let [result (take the-length stream-seq)]
			(println "fixed result" result)
			result)))
        )

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
					(println "doing: " (first rest-sections))
					(recur (rest rest-sections) (conj acc (parse-section to-chunk basis acc (first rest-sections)))))
					acc))))

(defn visualize-section [visualizer parsed-section]
    (do
        ; (prn "eph")
        (when visualizer
            (do
                ; (prn "ugg")
    	   (visualize-it visualizer parsed-section)))))

(defn visualize-binary [visualizers parsed-binary]
    (do
        (println "****************** START BINARY  ****************** ")
        (doall (map visualize-section visualizers parsed-binary))
        (println "****************** END BINARY  ****************** ")))

