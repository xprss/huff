# Hexastar Italian syllabification

`hyph_it_IT.dic` is based on the TeX Italian hyphenation tables by Claudio
Beccari and was converted by Giuseppe Bilotta. The LibreOffice distribution
identifies the Italian hyphenation patterns as LGPL-3.0 licensed.

Hexastar checks the project-owned corrections in
`hexastar-syllable-overrides.json` first; when no correction exists, it applies
these patterns with one-character edge limits.

The runtime implementation uses `io.github.nianna:hyphenator:1.1.0`, licensed
under LGPL-2.1-or-later. Its complete license text is included in the published
Maven artifact.

Sources:

- https://github.com/LibreOffice/dictionaries/tree/master/it_IT
- https://github.com/Nianna/hyphenator
- https://accademiadellacrusca.it/it/consulenza/divisione-in-sillabe/302
