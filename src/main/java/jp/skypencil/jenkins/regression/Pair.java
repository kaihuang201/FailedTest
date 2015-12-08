package jp.skypencil.jenkins.regression;

/**
 * 
 * @author Team FailedTest
 *
 *         This class uses X and Y to create a Tuple
 *
 * @param <X>
 * @param <Y>
 */
public class Pair<X, Y> {
	public final X first;
	public final Y second;

	public Pair(X x, Y y) {
		first = x;
		second = y;
	}
}
