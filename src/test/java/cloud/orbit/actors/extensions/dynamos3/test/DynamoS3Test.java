/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.extensions.dynamos3.test;

import org.junit.Assert;
import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.dynamodb.DynamoDBConfiguration;
import cloud.orbit.actors.extensions.dynamos3.DynamoS3StorageExtension;
import cloud.orbit.actors.extensions.s3.S3Configuration;
import cloud.orbit.actors.test.StorageTest;

/**
 * Created by joe@bioware.com on 2016-04-05.
 */
public class DynamoS3Test
{
    private static final String ACTOR_ID_SHORT = "ShortTestActor";
    private static final String ACTOR_ID_LONG = "LongTstActor";

    private static final String TEST_STRING_SHORT = "OrbitTestString1928374";
    private static final String TEST_STRING_LONG = new String(new char[9000000]).replace("\0", "X");

    private S3Configuration s3Configuration;
    private DynamoDBConfiguration dynamoDBConfiguration;
    private DynamoS3StorageExtension dynamoS3StorageExtension;
    private Stage stage;

    private void restartStage()
    {
        if(stage != null)
        {
            stage.stop().join();
            stage = null;
        }

        stage = new Stage.Builder().clusterName("dynamo-s3-test").extensions(dynamoS3StorageExtension).build();
        stage.start().join();
        stage.bind();
    }

    @Test
    public void testDynamoS3()
    {
        dynamoDBConfiguration = new DynamoDBConfiguration.Builder()
                .withCredentialType(cloud.orbit.actors.extensions.dynamodb.AmazonCredentialType.DEFAULT_PROVIDER_CHAIN)
                .build();

        s3Configuration = new S3Configuration.Builder()
                .withCredentialType(cloud.orbit.actors.extensions.s3.AmazonCredentialType.DEFAULT_PROVIDER_CHAIN)
                .build();

        dynamoS3StorageExtension = new DynamoS3StorageExtension(dynamoDBConfiguration, s3Configuration);

        dynamoS3StorageExtension.setS3BucketName("test-jhegarty");
        dynamoS3StorageExtension.setDefaultDynamoTableName("test-jhegarty");

        restartStage();

        Actor.getReference(TestActor.class, ACTOR_ID_SHORT).writeRecord(TEST_STRING_SHORT).join();

        String resultString = Actor.getReference(TestActor.class, ACTOR_ID_SHORT).getRecord().join();
        Assert.assertEquals(TEST_STRING_SHORT, resultString);

        restartStage();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_SHORT).getRecord().join();
        Assert.assertEquals(TEST_STRING_SHORT, resultString);

        Actor.getReference(TestActor.class, ACTOR_ID_SHORT).clearAllState();


        resultString = Actor.getReference(TestActor.class, ACTOR_ID_SHORT).getRecord().join();
        Assert.assertNull(resultString);

        restartStage();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_SHORT).getRecord().join();
        Assert.assertNull(resultString);

        Actor.getReference(TestActor.class, ACTOR_ID_LONG).writeRecord(TEST_STRING_LONG).join();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_LONG).getRecord().join();
        Assert.assertEquals(TEST_STRING_LONG, resultString);

        restartStage();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_LONG).getRecord().join();
        Assert.assertEquals(TEST_STRING_LONG, resultString);

        Actor.getReference(TestActor.class, ACTOR_ID_LONG).clearAllState();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_LONG).getRecord().join();
        Assert.assertNull(resultString);

        restartStage();

        resultString = Actor.getReference(TestActor.class, ACTOR_ID_LONG).getRecord().join();
        Assert.assertNull(resultString);
    }
}
