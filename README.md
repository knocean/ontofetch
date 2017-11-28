# ontofetch

ontofetch is a command line tool (Linux and OS X) for working with [Open Biomedical Ontologies](http://obofoundry.org/). It quickly downloads ontologies and their imports, extracts the metadata, and prepares a report of the operation.

## Usage

ontofetch should be downloaded from this repo. If you just want the executables, you only need the `target` directory.

The executable file lives at `target/ontofetch`. This is the exectuable for the jar version found in `target/uberjar`. Therefore, you can run ontofetch with either:

    $ java -jar target/ontofetch-0.1.0-standalone.jar [opts]

Or...

	$ target/ontofetch [opts]

You can also rebuild just by running `lein bin` in the main directory (see [lein-bin](https://github.com/Raynes/lein-bin) if you're curious). This requires [Leiningen](https://leiningen.org) 2.x.

## Options

ontofetch only accepts options at this time (no args):

	$ ./ontofetch --dir obi --purl http://purl.obolibrary.org/obo/obi.owl
    
(You can compress the created directory by adding a `--zip` flag)

This will create a directory 'obi'\* and download the ontology at the given PURL to it. The following files are also generated:
  * `obi-element.owl` - XML output of just the owl:Ontology element
  * `catalog-v001.xml` - allows Protègè to load local import files\*\*
  * `catalog.edn` - a running report of all ontofetch operations made in that directory
  * `report.html` - a formatted report of the catalog (above)

\* The directory should not already exist, and can only include letters, numbers, or underscores.

\*\* only created if there are direct imports in the downloaded ontology

## Testing

ontofetch is built using [Leiningen](https://leiningen.org) and includes plugins for testing. `lein test` is the easiest way to quickly run all tests, but running `lein cloverage` will also return a report of test coverage (see [cloverage](https://github.com/cloverage/cloverage)). Make sure you have the full test directory, as required test files live in `test/resources`.

## Issues

Please report any issues in our GitHub Issues.

## License

Copyright © 2017, Knocean Inc.

Distributed under the BSD 3-Clause License.
