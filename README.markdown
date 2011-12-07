# binpv

Library for specifying the format of a binary file, parsing binary files and printing a somewhat human readable format.  This currently reads the file and stores the entire thing in memory so it will not work for very large files.

Very *alpha* at the moment.  Use at your own risk.

## Usage

Just write some code.

### Specify the format of your binary file

Made up example that shows the various sections that can comprise the binary file:

    (def key-token-format (binary-protocol (ByteBasedChunker.)
        (section :KEY_TOKEN_ID,            (FixedLength. 2))
        (section :INCLUDES_PRIVATE_KEY,    (EnumeratedValue. 1 (IncludesPrivateKey.)))
        (section :PRIVATE_KEY,             (DependentValue. 256 (PrivateKeyPresent.)))
        (section :PUBLIC_KEY_LENGTH,       (FixedLength. 2))
        (section :PUBLIC_KEY,              (DependentFixedLength. (PublicKeyLength.)))
        (section :THROW_AWAY,              (VariableLength. (StopAt. [\F \G])))
        (section :THE_END,                 (EnumeratedValue. 2 (AllDone.)))))

You'll have to extend a few protocols yourself if you intend to use sections that are:

* enumerated values (to provide the possible values)
* dependent values (to declare which value from an earlier section determines if this section is present in the file)
* dependent fixed length (the length depends on the value of an earlier section) 
* variable length (only if you want something more sophisticated than a stop sequence)

Examples in test/binpv/test/core.clj include:

* IncludesPrivateKey
* PrivateKeyPresent
* PublicKeyLength
* AllDone

### Specify how each section will be rendered

Visualizers are completely separated from the declaration of the binary format.  This allows you to have as many visual
representations as you would like.  A command line visualizer and a GUI visualizer for example.

    (def cli-visualizer [(IntegerVisualizer.)
                        (IntegerVisualizer.)
                        (HexVisualizer.)
                        (IntegerVisualizer.)
                        (HexVisualizer.)
                        (HexVisualizer.)
                        (IntegerVisualizer.)])

Nested binary protocols use a BinaryProtocolVisualizer which is passed the list of visualizers to use. Replace '...'
with the actual visualizers of course.

    (def nested-visualizer (BinaryProtocolVisualizer.
                        [...]))

### Parse your binary file

    (def parsed (parse-binary (FileStreamWrapper. test-file) key-token-format))

### Render the parsed binary

    (visualize-binary cli-visualizer parsed)

### You can also nest binary protocols.  See test/binpv/test/am.clj for an example.

### See [http://blog.brweber2.com/blog/2011/12/03/a-binary-file-viewer/] for some additional documentation.

## License

Copyright (C) 2011 brweber2 

Distributed under the Eclipse Public License, the same as Clojure.
