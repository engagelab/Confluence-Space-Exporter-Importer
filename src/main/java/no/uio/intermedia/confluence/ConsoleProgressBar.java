package no.uio.intermedia.confluence;

public class ConsoleProgressBar {
	public static void main(String[] args) {
		char[] animationChars = new char[] { '|', '/', '-', '\\' };
		for (int i = 0; i <= 100; i++) {
			System.out.print("Processing: " + i + "% " + animationChars[i % 4]
					+ "\r");

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Processing: Done!          ");
	}
}