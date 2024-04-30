#!/bin/bash

start_rate=1
end_rate=100
win_count_one=0
win_count_two=0 
win_count_three=0

javac -cp "./lib/*:." @infexf.srcs

for rate in $(seq $start_rate $end_rate); do
    echo "Iteration: $rate"

    output=$(java -cp "./lib/*:." edu.cwru.sepia.Main2 data/labs/infexf/OneUnitSmallMaze.xml)
    echo "$output"

    if echo "$output" | grep -q "The enemy was destroyed, you win!"; then
        ((win_count_one++))
    fi
    
    wait
done

for rate in $(seq $start_rate $end_rate); do
    echo "Iteration: $rate"

    output=$(java -cp "./lib/*:." edu.cwru.sepia.Main2 data/labs/infexf/TwoUnitSmallMaze.xml)
    echo "$output"

    if echo "$output" | grep -q "The enemy was destroyed, you win!"; then
        ((win_count_two++))
    fi
    
    wait
done

for rate in $(seq $start_rate $end_rate); do
    echo "Iteration: $rate"

    output=$(java -cp "./lib/*:." edu.cwru.sepia.Main2 data/labs/infexf/BigMaze.xml)
    echo "$output"

    if echo "$output" | grep -q "The enemy was destroyed, you win!"; then
        ((win_count_three++))
    fi
    
    wait
done

echo "Total wins in One Unit $win_count_one"
echo "Total wins in Two Unit $win_count_two"
echo "Total wins in Big Maze $win_count_three"
