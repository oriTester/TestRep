package com.robot.ros

import android.util.Log
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import org.ros.node.Node
import org.ros.node.NodeMain
import org.ros.node.topic.Subscriber
import std_msgs.Int16

class BatteryListener : AbstractNodeMain(), NodeMain {
    companion object {
        private const val TAG = "BatteryListener"
        private const val SERVICE_NAME = "/rop/rop_battery"
        private const val GRAPH = "androidClient/Battery"
        const val NODE = "androidClient/BatteryListener"
    }

    var onDidChangeBatteryLevel: ((Int) -> Unit)? = null

    override fun getDefaultNodeName(): GraphName {
        return GraphName.of(GRAPH)
    }

    override fun onStart(connectedNode: ConnectedNode) {
        val subscriber: Subscriber<Int16> =
            connectedNode.newSubscriber(SERVICE_NAME, Int16._TYPE)
        subscriber.addMessageListener { message ->
            onDidChangeBatteryLevel?.invoke(message.data.toInt())
        }
    }

    override fun onError(node: Node, throwable: Throwable) {
        Log.d(TAG, "onError")
    }
}