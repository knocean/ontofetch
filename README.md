# ontofetch

**ontofetch** is a command line tool (Linux and OS X) for working with [Open Biomedical Ontologies](http://obofoundry.org/). It can download ontologies and keep them updated, extract their metadata, and prepare reports.

## Download

First, make sure you have [Leiningen](https://leiningen.org) 2.x.

ontofetch should be downloaded from this repo. In order to get the executables, build the project running `lein bin` in the ontofetch directory (see [lein-bin](https://github.com/Raynes/lein-bin) if you're curious). This will produce a `target` directory.

The executable file lives at `target/ontofetch`. This is the exectuable for the jar version found in `target/uberjar`. Therefore, you can run ontofetch with either:

    $ java -jar target/uberjar/ontofetch-0.1.0-SNAPSHOT-standalone.jar [command] [options] <arguments>

Or...

	$ target/ontofetch [command] [options] <arguments>

(Or add the `target` directory to your `PATH` environment variable for easy access)

## Configuration

For most commands, ontofetch expects a `config.edn` file in the working directory. Any time you run a command without options or with the  `--project` flag, ontofetch looks for this file. or This file should contain details for each ontology project you wish to work on. It is structured as so:

    {:extracts "dir-for-ontology-elements"
     :serve-interval ms
     :projects
     [{:id "ontology-id-1"
       :dir "DateFormat"
       :url "path-to-ontology-1"}
      {:id "ontology-id-2"
       :dir "DateFormat"
       :url "path-to-ontology-2"}
      ...]}

the `extracts` entry before the `projects` vector specifies where any owl:Ontology elements retrieved through the extract command should be stored.

The `id` for each project will be used as the main project directory, we recommend setting this to the ontology's ID (i.e. Gene Ontology -> "go"). The `dir` is the subdirectory for each fetch, in [date and time pattern string](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) format (note that `-` is not currently allowed in file path names, but it is acceptable to create more nested directories by including `/`). Finally, the `url` is the path to the ontology online (most likely a PURL).

For example: 

    {:id "bfo"
     :dir "yyyy_MM_dd"
     :url "http://purl.obolibrary.org/obo/bfo.owl"}

## Usage

### fetch
Retrieves an ontology from a URL and all imports. Each operation's details are stored in catalog.edn. Supported formats include: RDF/XML, Turtle, OWL/XML, Manchester.

    $ ontofetch fetch [options] <arguments>

fetch requires options, unlike all other commands in which running without options runs them over all configured projects.

  * `fetch --dir <arg> --url <arg>`: fetch the URL to the given directory
  * `fetch --project <arg> --dir <arg> --url <arg>`: fetch the URL to given project/directory
  * `fetch --project <arg>`: fetch configured project to dated directory in project folder

If the ontology has imports, a `catalog-v001.xml` file will be generated for Protégé, pointing to the local path of each import. Each fetch operation (including those initiated by `update`) is logged in `catalog.edn`, which is placed in the working directory. An HTML report is also generated as `report.html`.

### extract

Pulls the owl:Ontology element from a directory or project and saves it in RDF/XML format as `[ont]-element.owl`.

    $ ontofetch extract [options] <arguments>

  * `extract`: extract from all configured projects
  * `extract --dir <arg>`: extract from ontology in directory to working directory
  * `extract --dir <arg> --extract-dir <arg>`: extract from ontology in given directory to --extracts directory
  * `extract --project <arg>`: extract last fetch in project to configured extracts directory
  
### serve

Continuously updates all projects in a directory on a schedule.

    $ ontofetch serve [options] <arguments>

serve always requires a configuration file.

  * `serve`: continuously run serve in current directory until killed
  * `serve --extracts`: include extraction of owl:Ontology element in serve
  * `serve --kill`: kills a running serve process
  
The schedule is set with the `:serve-interval` property in `config.edn`. This should be provided as an integer in milliseconds. If no `:serve-interval` is provided, the process will default to updating every 4 hours.

This will continue running until killed. Each time `serve` is run, a new, dated directory is created in the `reports` directory. This contains `fetches.html` with all fetch reports, and an HTML page for each project.

### status

Checks if a project (or projects) is up-to-date based on the HTTP headers of the resource (ETag and Last-Modified).

    $ ontofetch status [options] <arguments>

status always requires a configuration file.

  * `status`: get status of all configured projects
  * `status --project <arg>`: get status of configured project

### update

Runs status on a project (or projects), then fetches if necessary.

    $ ontofetch update [options] <arguments>

update always requires a configuration file.

  * `update`: update all configured projects
  * `update --project <arg>`: update configured project

### Flags

* `-h, --help`: print usage information (ontofetch [command] --help)
* `-w, --working-dir <arg>`: set top-level working directory, defaults to current directory
* `-z, --zip`: compress results of fetch (valid for `fetch` and `update`)

## Testing

ontofetch is built using [Leiningen](https://leiningen.org) and includes plugins for testing. `lein test` is the easiest way to quickly run all tests, but running `lein cloverage` will also return a report of test coverage (see [cloverage](https://github.com/cloverage/cloverage)). Make sure you have the full test directory, as required test files live in `test/resources`.

## Issues

Please report any issues in our [GitHub Issues](https://github.com/knocean/ontofetch/issues).

## License

Copyright © 2017, Knocean Inc.

Distributed under the BSD 3-Clause License.
