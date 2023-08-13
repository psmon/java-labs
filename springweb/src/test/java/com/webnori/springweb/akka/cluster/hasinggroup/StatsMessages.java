package com.webnori.springweb.akka.cluster.hasinggroup;

import com.webnori.springweb.example.akka.actors.cluster.MySerializable;

public interface StatsMessages {

    public static class StatsJob implements MySerializable {
        private final String text;

        public StatsJob(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class StatsResult implements MySerializable {
        private final double meanWordLength;

        public StatsResult(double meanWordLength) {
            this.meanWordLength = meanWordLength;
        }

        public double getMeanWordLength() {
            return meanWordLength;
        }

        @Override
        public String toString() {
            return "meanWordLength: " + meanWordLength;
        }
    }

    public static class JobFailed implements MySerializable {
        private final String reason;

        public JobFailed(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "JobFailed(" + reason + ")";
        }
    }
}
