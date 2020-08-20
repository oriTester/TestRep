package com.robot.ros

import org.ros.concurrent.CancellableLoop
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.topic.Publisher
import org.ros.node.topic.Subscriber
import xtend_rop.BrainCommand


internal class BrainPublisher : AbstractNodeMain() {
    companion object {
        private const val TAG = "BrainPublisher"
        private const val SERVICE_NAME = "/rop/rop_brain"
        private const val MSG_SOURCE = "App"
        private const val NODE = "androidClient/BrainPublisher"
    }

    private lateinit var publisher: Publisher<BrainCommand>

    override fun getDefaultNodeName(): GraphName {
        return GraphName.of(NODE)
    }

    override fun onStart(connectedNode: ConnectedNode) {
        publisher = connectedNode.newPublisher(SERVICE_NAME, BrainCommand._TYPE)

        connectedNode.executeCancellableLoop(object : CancellableLoop() {
            override fun setup() {
                publisher.latchMode = false
            }

            @Throws(InterruptedException::class)
            override fun loop() {
                Thread.sleep(1000)
            }
        })
    }

    fun sendCommand(commandID:String?, param:String?, result: ((Boolean, String) -> Unit)) {
        val count = publisher.numberOfSubscribers
        if (count!=0) {
            val msg = publisher.newMessage()
            msg.commandID = commandID
            msg.source = MSG_SOURCE
            msg.params = param
            publisher.publish(msg)
            result(true, "Command sent")
        }
        else {
            result(false, "No subscribers found")
        }
    }

    fun move(param:String, result: ((Boolean, String) -> Unit)) {
        val count = publisher.numberOfSubscribers
        if (count!=0) {
            val msg = publisher.newMessage()
            msg.commandID = "Move"
            msg.source = MSG_SOURCE
            msg.params = param
            publisher.publish(msg)
            result(true, "Command sent")
        }
        else {
            result(false, "No subscribers found")
        }
    }

    fun goToRoom(room:String, result: ((Boolean, String) -> Unit)) {
        val count = publisher.numberOfSubscribers
        if (count!=0) {
            val msg = publisher.newMessage()
            msg.commandID = "Navigate"
            msg.source = MSG_SOURCE
            msg.params = "go_to_position $room"
            publisher.publish(msg)
            result(true, "Command sent")
        }
        else {
            result(false, "No subscribers found")
        }
    }
}