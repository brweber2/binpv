# binpv

Library for specifying the format of a binary file.
Very *alpha* at the moment.  Use at your own risk.

## Usage

Just write some code.

### Specify the format of your binary file

Made up example that shows the various sections that can make up the binary file:

(def key-token-format (binary-protocol (ByteBasedChunker.)
    (section :KEY_TOKEN_ID,            (FixedLength. 2))
    (section :INCLUDES_PRIVATE_KEY,    (EnumeratedValue. 1 (IncludesPrivateKey.)))
    (section :PRIVATE_KEY,             (DependentValue. 256 (PrivateKeyPresent.)))
    (section :PUBLIC_KEY_LENGTH,       (FixedLength. 2))
    (section :PUBLIC_KEY,              (DependentFixedLength. (PublicKeyLength.)))
    (section :THROW_AWAY,              (VariableLength. (StopAt. [\F \G])))
    (section :THE_END,                 (EnumeratedValue. 2 (AllDone.)))))

### Parse your binary file

(def parsed (parse-binary (FileStreamWrapper. test-file) key-token-format))

### Render the parsed binary

(visualize-binary (take 7 (repeat (HexVisualizer.))) parsed)

## License

Copyright (C) 2011 brweber2 

Distributed under the Eclipse Public License, the same as Clojure.
