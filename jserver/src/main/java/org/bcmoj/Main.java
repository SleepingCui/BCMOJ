package org.bcmoj;

import org.bcmoj.bootstrap.Bootstrap;

/**
 * BCMOJ Judge Server.
 * <p>
 * Starts the application by delegating to {@link org.bcmoj.bootstrap.Bootstrap}.
 *
 * @author SleepingCui
 * @version ${project.version}
 */
public class Main {
    public static void main(String[] args) {
        Bootstrap.run(args);
    }
}
