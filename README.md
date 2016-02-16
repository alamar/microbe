microbe
==

This program is a computer model that simulates evolution of a finite population of unicellular organisms.

You need Java Runtime Environment >= 7.

Models, simulation results, charts and backups reside in `MODELS` directory

To run models:

    model.bat {model name without .properties} [rng seed as number] [property=value] [property=value]...

or `./model.sh` under UNIX.

for example, `model.bat monoploid 12345 steps=1500`

It will produce `{model-with-seed-and-properties}.txt` with simulation results as well as printing steps on stdout.
It will also produce `{model-with-seed-and-properties}.png` with average fitness chart.

See `default.properties` in MODELS for a range of model parameters. Create your own `*.properties` files to override defaults.

You can put multiple properties files in subdirectory under MODELS. `model.bat {subdir}` will simulate them one by one.

Combining charts:

    merge.bat [generations as number] {model-with-seed-and-properties} {another-model,yet-other-model...} {output}

or `./merge.sh` under UNIX

Will read `.txt` files from MODELS and produce `{output}.png` in MODELS with multiple charts.

You can provide generations #, only those will be drawn. Otherwise it's longest output + some padding.

for example:

    merge.bat 650 average-monoploid diploid,diploid-inexact triploid,triploid-inexact ploidy

Will produce a chart with 650 steps of average monoploid, two diploids in similar colors (red), two triploid in similar colors (green).

For development, you need Maven. `mvn package` will compile source, run some tests, download dependencies and package compiled files.

Use `mvn dependency:copy-dependencies` and `mvn license:update-project-license` for maintenance.

![Ploidy chart](https://raw.github.com/alamar/microbe/master/models/ploidy.png)
