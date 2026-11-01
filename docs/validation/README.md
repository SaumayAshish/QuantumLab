# Independent validation

These scripts compute reference values for parts of QuantumLab with NumPy, without using any of
the Java code, and compare them with the values and claims written into the Java unit tests.

They do **not** run the Java classes. Their job is to show that the numbers the tests rely on are
right: a test that asserts `0.0558614…` is only as good as that number, and here it is derived a
second way.

| Script                       | Java tests it backs                | What is computed independently                                                                 |
|------------------------------|------------------------------------|-------------------------------------------------------------------------------------------------|
| `infinite_well.py`           | `InfiniteWellTest`                 | Dense diagonalisation of the finite-difference matrix, the observed order, normalisation of the exact states |
| `barrier_transmission.py`    | `RectangularBarrierTest`           | Transmission by solving the four plane-wave matching equations; the closed form; resonances; the thick-barrier estimate |
| `dephasing.py`               | `DephasingTest`                    | The density matrix, Born probabilities, purity, entropy and Bloch components with plain matrix algebra |

## Running

Requires Python 3 and NumPy (`python -m pip install numpy`). From this folder:

```
python infinite_well.py
python barrier_transmission.py
python dephasing.py
```

Each prints one block per comparison: `ok` when the independent value and the Java test's value
agree within the stated tolerance, `FAIL` otherwise. The exit status is 1 if anything failed, so
the scripts can be run from a shell or CI step.

## Limits

- Only the quantities listed above are covered. The Schrödinger convergence study, the wave
  packet, the composite system and CHSH are tested in Java but have no script here yet.
- The infinite-well script checks the closed-form finite-difference eigenvalue formula against
  dense diagonalisation up to N = 640 and then uses the formula at N = 100 000, where a dense
  matrix is impractical.
- Agreement within tolerance is evidence, not proof: it shows two methods give the same numbers
  for the cases tested.
