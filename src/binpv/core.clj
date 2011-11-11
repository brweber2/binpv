(ns binpv.core)

(defn get-sections [sections id]
    (filter id sections))

(defn get-section [sections id]
    (take 1 (get-sections sections id)))

(def section-keys #{:ID :FIXED_LENGTH :DEPENDENT_FIXED_LENGTH :VARIABLE_LENGTH :DEPENDENT_FUN :ENUM_FUN :DESCRIPTION})


(defn section 
	"Valid section keys:
	:ID
		Can be a string, symbol or keyword.
	
	; only one of the following:
	:FIXED_LENGTH
	:DEPENDENT_FIXED_LENGTH
		Takes a function that returns the length of the section.  
		The function is passed a sequence the sections parsed so far.
	:VARIABLE_LENGTH
		Takes a function that returns true when it is time to stop reading the section.
		The function takes one chunk and a context.  It must return a seq of a boolean and a context map.
		The context map allows state to be passed from chunk to chunk since they are read one at a time.

	:DEPENDENT_FUN 
		Function called that will determine if this section is skipped or present.
		The function takes a sequence of sections that have been parsed so far.
	:ENUM_FUN
		Function that takes a sequence of the chunks read.

	:DESCRIPTION"
	[& section-info]
	; todo add post condition that validates keys and values?
	(apply hash-map section-info))

(defn parse-section [stream section]
	)

(defn parse-binary [binary-stream binary-protocol]
	(map (partial parse-section binary-stream) (:sections binary-protocol)))

(defn visualize-section [section]
	)

(defn visualize-binary [parsed-binary]
	)

(defn binp [basis-keyword, basis & sections]
	^{basis-keyword basis} sections)

(defmacro defbinp [name-to-def, basis-keyword, basis & sections ]
	`(def ~name-to-def (binp ~basis-keyword ~basis ~@sections)))
