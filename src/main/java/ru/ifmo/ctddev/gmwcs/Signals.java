package ru.ifmo.ctddev.gmwcs;

import ru.ifmo.ctddev.gmwcs.graph.Unit;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Signals {
    private List<Set<Unit>> sets;
    private Map<Unit, List<Integer>> unitsSets;
    private List<OptionalDouble> weights;

    public Signals() {
        sets = new ArrayList<>();
        unitsSets = new HashMap<>();
        weights = new ArrayList<>();
    }

    public Signals negativeSignals() {
        Signals s = new Signals();
        s.sets = new ArrayList<>(sets);
        s.weights = new ArrayList<>(weights);
        for (Map.Entry<Unit, List<Integer>> kvp : unitsSets.entrySet()) {
            Unit unit = kvp.getKey();
            List<Integer> unitSet = kvp.getValue().stream()
                    .filter(v -> weight(v) < 0).collect(Collectors.toList());
            s.unitsSets.put(unit, unitSet);
        }
        return s;
    }


    public Signals(Signals signals, Set<Unit> subset) {
        this();
        for (Unit unit : subset) {
            unitsSets.put(unit, new ArrayList<>());
        }
        int j = 0;
        for (int i = 0; i < signals.size(); i++) {
            Set<Unit> set = new HashSet<>();
            for (Unit unit : signals.set(i)) {
                if (subset.contains(unit)) {
                    set.add(unit);
                    unitsSets.get(unit).add(j);
                }
            }
            if (!set.isEmpty()) {
                sets.add(set);
                weights.add(OptionalDouble.of(signals.weight(i)));
                j++;
            }
        }
    }

    public int size() {
        return sets.size();
    }

    public double weight(int num) {
        assert weights.get(num).isPresent();
        return weights.get(num).getAsDouble();
    }


    public double minSum(Collection<? extends Unit> units) {
        return minSum(units.toArray(new Unit[0]));
    }

    public double maxSum(Collection<? extends Unit> units) {
        return maxSum(units.toArray(new Unit[0]));
    }

    public double maxSum(Unit... units) {
        return sumByPredicate(units, w -> w > 0);
    }

    public double minSum(Unit... units) {
        return sumByPredicate(units, w -> w < 0);
    }

    private double sumByPredicate(Unit[] units, Predicate<Double> pred) {
        return unitSets(Arrays.stream(units))
                .mapToDouble(this::weight)
                .filter(pred::test).sum();
    }


    public double weight(Unit unit) {
        return unitsSets.get(unit).stream().mapToDouble(this::weight).sum();
    }

    public void join(Unit what, Unit with) {
        List<Integer> x = unitsSets.get(what);
        List<Integer> main = unitsSets.get(with);
        int i = 0, j = 0;
        List<Integer> result = new ArrayList<>();
        while (i != x.size() || j != main.size()) {
            int set;
            if (!(j == main.size()) && (i == x.size() || main.get(j) < x.get(i))) {
                set = main.get(j);
                ++j;
            } else {
                set = x.get(i);
                sets.get(set).remove(what);
                sets.get(set).add(with);
                ++i;
            }
            if (result.isEmpty() || result.get(result.size() - 1) != set) {
                result.add(set);
            }
        }
        unitsSets.put(with, result);
        result = new ArrayList<>(new HashSet<>(unitsSets.get(with)));
        unitsSets.put(with, result);
        unitsSets.remove(what);
    }

    public Map<Unit, List<Integer>> unitSets() {
        Map<Unit, List<Integer>> result = new HashMap<>();
        for (Unit unit : unitsSets.keySet()) {
            result.put(unit, unitsSets.get(unit));
        }
        return result;
    }

    public List<Integer> unitSets(Collection<? extends Unit> units) {
        return unitSets(units.stream()).collect(Collectors.toList());
    }

    public double weightSum(Collection<Integer> signals) {
        return signals.stream().distinct().mapToDouble(this::weight).sum();
    }

    public List<Integer> positiveUnitSets(Collection<? extends Unit> units, boolean distinct) {
        if (distinct) {
            return positiveUnitSets(units);
        } else {
            return units.stream()
                    .map(this::unitSets)
                    .flatMap(Collection::stream).filter(u -> weight(u) >= 0)
                    .collect(Collectors.toList());
        }

    }

    public boolean bijection(Unit unit) {
        List<Integer> ss = unitSets(unit);
        return ss.stream().allMatch(s -> set(s).size() == 1);
    }

    public List<Integer> positiveUnitSets(Collection<? extends Unit> units) {
        return unitSets(units.stream()).filter(u -> weight(u) >= 0).collect(Collectors.toList());
    }


    public List<Integer> negativeUnitSets(Collection<? extends Unit> units) {
        return unitSets(units.stream()).filter(u -> weight(u) < 0).collect(Collectors.toList());
    }

    public List<Integer> negativeUnitSets(Unit unit) {
        return negativeUnitSets(Collections.singletonList(unit));
    }

    public List<Integer> positiveUnitSets(Unit unit) {
        return positiveUnitSets(Collections.singletonList(unit));
    }

    private Stream<Integer> unitSets(Stream<? extends Unit> units) {
        return units.map(this::unitSets)
                .flatMap(Collection::stream)
                .distinct();
    }

    public List<Integer> unitSets(Unit unit) {
        return Collections.unmodifiableList(unitsSets.get(unit));
    }

    public List<Unit> set(int num) {
        List<Unit> result = new ArrayList<>();
        result.addAll(sets.get(num));
        return result;
    }

    public void add(Unit unit, int signalTo) {
        sets.get(signalTo).add(unit);
        ensureLink(unit, signalTo);
    }

    private int add(Unit unit) {
        Set<Unit> s = new HashSet<>();
        s.add(unit);
        sets.add(s);
        weights.add(OptionalDouble.empty());
        int num = sets.size() - 1;
        ensureLink(unit, num);
        return num;
    }

    public int addAndSetWeight(Unit unit, Double weight) {
        int num = add(unit);
        setWeight(num, weight);
        return num;
    }

    public void setWeight(int set, double weight) {
        weights.set(set, OptionalDouble.of(weight));
    }

    private void ensureLink(Unit unit, int signal) {
        if (unitsSets.containsKey(unit)) {
            unitsSets.get(unit).add(signal);
        } else {
            List<Integer> l = new ArrayList<>();
            unitsSets.put(unit, l);
            l.add(signal);
        }
    }
}
