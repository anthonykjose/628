#!/bin/bash
# Master Script to create 5-fold training and test data for inputting to classifier and calculate average accuracies for classification. 

#Lowecasing
sed -e 's/\(.*\)/\L\1/' hotel_deceptive > hotel_deceptive_new
sed -e 's/\(.*\)/\L\1/' hotel_truthful > hotel_truthful_new

#Separating punctuations
sed -i "s/\([\,\.\(\)\!\;\"\:\']\)/ \1 /g" hotel_deceptive_new
sed -i 's/ \+/ /g' hotel_deceptive_new

sed -i "s/\([\,\.\(\)\!\;\"\:\']\)/ \1 /g" hotel_truthful_new
sed -i 's/ \+/ /g' hotel_truthful_new

javac TrainAndTest.java
java TrainAndTest $1 $2

#Create folds for training and test
for(( i=0; i<5; i++ ))
do
	sed -n -e $(( 1 + 80*i )),$(( 80 + 80*i ))p -e $(( 401 + 80*i )),$(( 480 + 80*i ))p input.txt > fold-$(( i + 1 ))-test
	sed -e $(( 1 + 80*i )),$(( 80 + 80*i ))d -e $(( 401 + 80*i )),$(( 480 + 80*i ))d input.txt > fold-$(( i + 1 ))-train
done

#Run the classifier
for(( i=0; i<5; i++ ))
do
	for(( j=0; j<4; j++ ))
	do
		echo "Creating Train-Test Sub-Folds for Fold $((i+1)) - Iteration : $((j+1)) ..";
		sed -n -e $(( 1 + 80*j )),$(( 80 + 80*i ))p -e $(( 321 + 80*i )),$(( 400 + 80*i ))p fold-$(( i + 1 ))-train > fold-$(( i + 1 ))$(( j + 1 ))-test
		sed -e $(( 1 + 80*i )),$(( 80 + 80*i ))d -e $(( 321 + 80*i )),$(( 400 + 80*i ))d fold-$(( i + 1 ))-train > fold-$(( i + 1 ))$(( j + 1 ))-train

		c=0.001;	
		for(( k=0; k<4; k++ ))
		do
			c=`echo "$c * 10" | bc`;
			echo "Nested Classification : Fold : $((i+1)) Subfold : $((j+1)) C-Parameter : $c";
			svm_learn -c $c fold-$(( i + 1 ))$(( j + 1 ))-train fold-$(( i + 1 ))$(( j + 1 ))-c$c-model.model > trash-out.txt
			svm_classify fold-$(( i + 1 ))$(( j + 1 ))-test fold-$(( i + 1 ))$(( j + 1 ))-c$c-model.model > out-c$c-$i$j.txt
		done 
	done

c1=0;
max_accuracy=0;

	c=0.001;	
	for(( k=0; k<4; k++ ))
	do
		avg_c=0;
		c=`echo "$c * 10" | bc`;
		for(( j=0; j<4; j++ ))
		do
			for x in `grep "Accuracy" out-c$c-$i$j.txt | grep -o "[0-9]*.[0-9]*%" | sed 's/\%//g'`
			do 
				avg_c=`echo "$avg_c + $x" | bc`;
			done
		done 
		avg_c=`echo "scale=2; $avg_c / 4" | bc`;

		if [ $(echo "$avg_c > $max_accuracy" | bc) -eq 1 ] ; then
			max_accuracy=$avg_c;
			c1=$c;		
		fi
		echo "Average Accuracy c = $c :: $avg_c";
	done
	echo "The BEST C IS : $c1";

rm out*;
#liblinear-train -c 0.01 fold-$(( i + 1 ))-train
echo "Training Classifier with C : $c1 for Fold : $((i+1))";
svm_learn -c $c1 fold-$(( i + 1 ))-train fold-$(( i + 1 ))-model.model > out-train.txt
mallet/bin/mallet import-svmlight --input fold-$((i+1))-train fold-$((i+1))-test --output train-fold-$((i+1)).mallet test-fold-$((i+1)).mallet
#svm-train -t 0 fold-$(( i + 1 ))-train fold-$(( i + 1 ))-model.model

done 

print > out.txt

for(( i=0; i<5; i++ ))
do
 	#liblinear-predict fold-$(( i + 1 ))-test fold-$(( i + 1 ))-train.model fold-$(( i + 1 )).output 
	svm_classify fold-$(( i + 1 ))-test fold-$(( i + 1 ))-model.model fold-$(( i + 1 ))-SVMlight.output >> out.txt
	mallet/bin/mallet train-classifier --training-file train-fold-$((i+1)).mallet --testing-file test-fold-$((i+1)).mallet --trainer MaxEnt >> maxent-out
	#svm-predict fold-$(( i + 1 ))-test fold-$(( i + 1 ))-model.model fold-$(( i + 1 )).output
done 

echo "--------------------------------------"
echo "				SVMlight				" 
echo "--------------------------------------"
echo "Printing Accuracies for each Fold ...."
total=0
for x in `grep "Accuracy" out.txt | grep -o "[0-9]*.[0-9]*%" | sed 's/\%//g'`
do 
	echo "Fold : $x%"
	total=`echo "$total + $x" | bc`
done

average_accuracy=`echo "scale=2; $total/5" | bc`

echo "Average Accuracy : $average_accuracy%"
echo "--------------------------------------"

echo "--------------------------------------"
echo "				MAXIMUM ENTROPY			" 
echo "--------------------------------------"
echo "Printing Accuracies for each Fold ...."
total=0
for x in `grep -o "test data accuracy= [0-9]*.[0-9]*" maxent-out | sed "s/test data accuracy= //g"`
do 
	echo "Fold : $x%"
	total=`echo "$total + $x" | bc`
done

maxent_average_accuracy=`echo "scale=2; ($total/5)*100" | bc`

echo "Average Accuracy : $maxent_average_accuracy%"
echo "--------------------------------------"

total_tp=0 
total_tn=0 
total_fp=0 
total_fn=0
i=0
for x in `grep "0 -1" maxent-out | sed "s/0 -1//g" | sed "s/|80//g"` 
do  
	if [ $i -eq 1 ] ; then
			total_fp=`echo "$total_fp + $x" | bc`
			i=`echo "$i - 1" | bc`				
	else	
			total_tn=`echo "$total_tn + $x" | bc`
			i=`echo "$i + 1" | bc`
	fi
done

i=0
for x in `grep "1  1" maxent-out | sed "s/1  1//g" | sed "s/|80//g"` 
do  
	if [ $i -eq 1 ] ; then
			total_tp=`echo "$total_tp + $x" | bc`
			i=`echo "$i - 1" | bc`				
	else	
			total_fn=`echo "$total_fn + $x" | bc`
			i=`echo "$i + 1" | bc`
	fi
done

	echo "TOTAL TP : $total_tp"
	echo "TOTAL tn : $total_tn"
	echo "TOTAL fp : $total_fp"
	echo "TOTAL fn : $total_fn"

	precision_deceptive=$(echo "scale=4; ( $total_tp / ( $total_tp + $total_fp ) ) * 100" | bc)
	recall_deceptive=$(echo "scale=4; ( $total_tp / ( $total_tp + $total_fn ) ) * 100" | bc)
	precision_truthful=$(echo "scale=4; ( $total_tn / ( $total_tn + $total_fn ) ) * 100" | bc)
	recall_truthful=$(echo "scale=4; ( $total_tn / ( $total_tn + $total_fp ) ) * 100" | bc)

	#avg_precision_deceptive=`echo "scale=2; $avg_precision_deceptive / 5" | bc`
	#avg_recall_deceptive=`echo "scale=2; $avg_recall_deceptive / 5" | bc`
	#avg_precision_truthful=`echo "scale=2; $avg_precision_truthful / 5" | bc`
	#avg_recall_truthful=`echo "scale=2; $avg_recall_truthful / 5" | bc`


echo "------------------------------------------------"
echo "				MAXIMUM ENTROPY					  " 
echo "------------------------------------------------"
echo "Average Accuracy : $maxent_average_accuracy%"
echo "Precision Deceptive : $precision_deceptive"
echo "Recall Deceptive : $recall_deceptive"
echo "Precision Truthful : $precision_truthful"
echo "Recall Truthful : $recall_truthful"
echo "------------------------------------------------"



#avg_precision_deceptive=0
#avg_recall_deceptive=0
#avg_precision_truthful=0
#avg_recall_truthful=0

total_tp=0 
total_tn=0 
total_fp=0 
total_fn=0

for (( i=0; i<5; i++ ))
do
	tp=0
	fn=0
	for x in `sed -n -e 81,160p fold-$(( i + 1 ))-SVMlight.output`
	do 
		if [ $(echo "$x < 0" | bc) -eq 1 ] ; then
			fn=`echo "$fn + 1" | bc`		
		else	
			tp=`echo "$tp + 1" | bc`
		fi
	done

	tn=0
	fp=0
	for x in `sed -n -e 1,80p fold-$(( i + 1 ))-SVMlight.output`
	do 
		if [ $(echo "$x > 0" | bc) -eq 1 ] ; then
			fp=`echo "$fp + 1" | bc`		
		else	
			tn=`echo "$tn + 1" | bc`
		fi
	done

	total_tp=`echo "$total_tp + $tp" | bc`
	total_tn=`echo "$total_tn + $tn" | bc`
	total_fp=`echo "$total_fp + $fp" | bc`
	total_fn=`echo "$total_fn + $fn" | bc`


	#echo "Precision-deceptive : $precision_deceptive"
	#echo "Recall-deceptive : $recall_deceptive"
	#echo "Precision-truthful : $precision_truthful"
	#echo "Recall-truthful : $recall_truthful"
	
	#avg_precision_deceptive=`echo "$avg_precision_deceptive + $precision_deceptive" | bc`
	#avg_recall_deceptive=`echo "$avg_recall_deceptive + $recall_deceptive" | bc`
	#avg_precision_truthful=`echo "$avg_precision_truthful + $precision_truthful" | bc`
	#avg_recall_truthful=`echo "$avg_recall_truthful + $recall_truthful" | bc`
done

	precision_deceptive=$(echo "scale=4; ( $total_tp / ( $total_tp + $total_fp ) ) * 100" | bc)
	recall_deceptive=$(echo "scale=4; ( $total_tp / ( $total_tp + $total_fn ) ) * 100" | bc)
	precision_truthful=$(echo "scale=4; ( $total_tn / ( $total_tn + $total_fn ) ) * 100" | bc)
	recall_truthful=$(echo "scale=4; ( $total_tn / ( $total_tn + $total_fp ) ) * 100" | bc)

	#avg_precision_deceptive=`echo "scale=2; $avg_precision_deceptive / 5" | bc`
	#avg_recall_deceptive=`echo "scale=2; $avg_recall_deceptive / 5" | bc`
	#avg_precision_truthful=`echo "scale=2; $avg_precision_truthful / 5" | bc`
	#avg_recall_truthful=`echo "scale=2; $avg_recall_truthful / 5" | bc`

echo "------------------------------------------------"
echo "				SVMlight						  " 
echo "------------------------------------------------"
echo "Average Accuracy : $average_accuracy%"
echo "Precision Deceptive : $precision_deceptive"
echo "Recall Deceptive : $recall_deceptive"
echo "Precision Truthful : $precision_truthful"
echo "Recall Truthful : $recall_truthful"
echo "------------------------------------------------"
	
rm fold*
rm maxent-out
rm train-fold*
rm test-fold*