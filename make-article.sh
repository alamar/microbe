#!/bin/sh

rm -f models/fig*.png models/fig*.csv models/fig*/*.png models/fig*/*.csv

echo fig01 fig03 fig04 fig05 fig06 fig07 fig08 fig09 fig10 | xargs -d ' ' -P 3 -L 1 ./model.sh

./merge.sh 1500 fig01/monoploid fig01/2-ploid fig01/6-ploid fig01/18-ploid fig01

./merge.sh 2000 fig03/monoploid fig03/2-ploid fig03/6-ploid fig03/18-ploid fig03

cp models/fig04/variable_ploidy.png models/fig04.png

./merge.sh 1500 fig05/6-ploid fig05/6-ploid_D20% fig05/6-ploid_D50% fig05

./merge.sh 1150 fig06/2-ploid fig06/2-ploid_conversion01 fig06/2-ploid_conversion1 fig06a
./merge.sh 1150 fig06/6-ploid fig06/6-ploid_conversion01 fig06/6-ploid_conversion1 fig06/6-ploid_conversion4 fig06b
./merge.sh fig06/18-ploid fig06/18-ploid_conversion1 fig06/18-ploid_conversion4 fig06/18-ploid_conversion8 fig06c

./merge.sh 1000 fig07/monoploid fig07/monoploid_LGT1 fig07/monoploid_LGT4 fig07a
./merge.sh 1000 fig07/2-ploid fig07/2-ploid_LGT01 fig07/2-ploid_LGT1 fig07b
./merge.sh 1000 fig07/6-ploid fig07/6-ploid_LGT01 fig07/6-ploid_LGT1 fig07/6-ploid_LGT4 fig07c
./merge.sh 1000 fig07/18-ploid fig07/18-ploid_LGT1 fig07/18-ploid_LGT4 fig07/18-ploid_LGT8 fig07d

./merge.sh 1000 fig08/6-ploid fig08/6-ploid_crossing1 fig08/6-ploid_LGT02 fig08/6-ploid_LGT02_crossing1 fig08a
./merge.sh 1000 fig08/6-ploid fig08/6-ploid_conversion1 fig08/6-ploid_LGT02 fig08/6-ploid_LGT02_conversion1 fig08b

./merge.sh 700 fig09/2-ploid fig09/2-ploid_chr-exchange05 fig09/2-ploid_crossing1 fig09/2-ploid_chr-exchange05_crossing1 fig09a
./merge.sh 700 fig09/6-ploid fig09/6-ploid_chr-exchange05 fig09/6-ploid_crossing1 fig09/6-ploid_chr-exchange05_crossing1 fig09b
./merge.sh 700 fig09/18-ploid fig09/18-ploid_chr-exchange05 fig09/18-ploid_crossing1 fig09/18-ploid_chr-exchange05_crossing1 fig09c

./merge.sh 1000 fig10/monoploid fig10/2-ploid,fig10/2-ploid_mitosis fig10/6-ploid,fig10/6-ploid_mitosis fig10/18-ploid,fig10/18-ploid_mitosis fig10a
./merge.sh 1000 fig10/monoploid_LGT1 fig10/2-ploid_mitosis_LGT1 fig10/6-ploid_mitosis_LGT1 fig10/18-ploid_mitosis_LGT1 fig10b
./merge.sh 1000 fig10/18-ploid_mitosis fig10/18-ploid_mitosis_chr-exchange05 fig10/18-ploid_mitosis_crossing1 fig10/18-ploid_mitosis_chr-exchange05_crossing1 fig10c

