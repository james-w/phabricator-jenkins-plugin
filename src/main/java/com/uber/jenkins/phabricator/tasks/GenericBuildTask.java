// Copyright (c) 2015 Uber Technologies, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.uber.jenkins.phabricator.tasks;

import com.uber.jenkins.phabricator.CodeCoverageMetrics;
import com.uber.jenkins.phabricator.uberalls.UberallsClient;
import com.uber.jenkins.phabricator.utils.CommonUtils;
import com.uber.jenkins.phabricator.utils.Logger;
import hudson.plugins.cobertura.targets.CoverageResult;

/**
 * Generic build task.
 */
public class GenericBuildTask extends Task {

    protected UberallsClient uberallsClient;
    protected CoverageResult coverageResult;
    protected boolean uberallsEnabled;
    protected String branch;
    protected String commitSha;

    /**
     * GenericBuildTask constructor.
     * @param logger The logger.
     * @param uberallsClient The uberalls client.
     * @param coverageResult The coverage result.
     * @param uberallsEnabled Whether uberalls is enabled.
     * @param branch The branch.
     * @param commitSha The commit sha.
     */
    public GenericBuildTask(Logger logger, UberallsClient uberallsClient,
                            CoverageResult coverageResult, boolean uberallsEnabled,
                            String branch, String commitSha) {
        super(logger);
        this.uberallsClient = uberallsClient;
        this.coverageResult = coverageResult;
        this.uberallsEnabled = uberallsEnabled;
        this.branch = branch;
        this.commitSha = commitSha;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTag() {
        return "generic-build";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() {
        // Handle bad input.
        if (coverageResult == null) {
            info("Coverage result not found. Ignoring build.");
            result = Result.IGNORED;
        } else if (!uberallsEnabled || CommonUtils.isBlank(uberallsClient.getBaseURL())) {
            info("Uberalls not configured. Skipping build.");
            result = Result.SKIPPED;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() {
        if (result == Result.UNKNWON) {
            CodeCoverageMetrics codeCoverageMetrics = new CodeCoverageMetrics(coverageResult);
            if (!CommonUtils.isBlank(commitSha) && codeCoverageMetrics.isValid()) {
                info(String.format("Sending coverage result for %s as %s", commitSha));
                uberallsClient.recordCoverage(commitSha, branch, codeCoverageMetrics);
                result = Result.SUCCESS;
            } else {
                info("No line coverage found. Ignoring build.");
                result = Result.IGNORED;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() {
        // Do nothing.
    }
}
