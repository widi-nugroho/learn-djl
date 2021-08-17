package linearregression.softmax.concise

import ai.djl.Device
import ai.djl.Model
import ai.djl.metric.Metrics
import ai.djl.ndarray.types.Shape
import ai.djl.nn.Blocks
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.EasyTrain
import ai.djl.training.Trainer
import ai.djl.training.dataset.Dataset
import ai.djl.training.dataset.RandomAccessDataset
import ai.djl.training.evaluator.Accuracy
import ai.djl.training.listener.TrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.optimizer.Optimizer
import ai.djl.training.tracker.Tracker
import linearregression.dataset.getDataset
import org.math.plot.Plot2DPanel
import java.awt.Color
import javax.swing.JFrame

class Lr_softmax_concise2 {
    lateinit var model:Model
    lateinit var net:SequentialBlock
    lateinit var loss:Loss
    lateinit var lrt: Tracker
    lateinit var sgd: Optimizer
    lateinit var trainer:Trainer

    constructor(lr:Float,numInputs:Long,numOutputs:Long){
        model=Model.newInstance("softmax-regression")
        net= SequentialBlock()
        net.add(Blocks.batchFlattenBlock(numInputs))
        net.add(Linear.builder().setUnits(numOutputs).build())

        model.block=net

        loss=Loss.softmaxCrossEntropyLoss()

        lrt=Tracker.fixed(lr)
        sgd=Optimizer.sgd().setLearningRateTracker(lrt).build()

        val config = DefaultTrainingConfig(loss)
            .optOptimizer(sgd) // Optimizer
            .optDevices(Device.getDevices(1)) // single GPU
            .addEvaluator(Accuracy()) // Model Accuracy
            .addTrainingListeners(*TrainingListener.Defaults.logging()) // Logging

        trainer=model.newTrainer(config)

        trainer.initialize(Shape(1,numInputs))
        trainer.metrics= Metrics()
    }
}

fun main(){
    val batchSize = 256
    val numInputs = 784.toLong()
    val numOutputs = 10.toLong()
    val numEpochs = 5
    val lr=0.1F
    val randomShuffle = true
    val yAccuracy = mutableListOf<Double>()
    val yLoss = mutableListOf<Double>()
    val xEpochs = mutableListOf<Double>()

    val k=Lr_softmax_concise2(lr,numInputs,numOutputs)

    val trainingSet: RandomAccessDataset = getDataset(Dataset.Usage.TRAIN, batchSize, randomShuffle)
    val validationSet: RandomAccessDataset = getDataset(Dataset.Usage.TEST, batchSize, false)

    EasyTrain.fit(k.trainer, numEpochs, trainingSet, validationSet)

    val epochAccuracy = k.trainer.metrics.getMetric("train_epoch_Accuracy")
    val epochLoss = k.trainer.metrics.getMetric("train_epoch_SoftmaxCrossEntropyLoss")

    k.trainer.trainingResult

    for (epoch in 0..numEpochs-1){
        xEpochs.add(epoch.toDouble())
        yAccuracy.add(epochAccuracy[epoch].value.toDouble())
        yLoss.add(epochLoss[epoch].value.toDouble())
    }

    val plot = Plot2DPanel()
    plot.addLinePlot("Accuracy", Color.RED,xEpochs.toDoubleArray(),yAccuracy.toDoubleArray())
    plot.addLinePlot("Loss", Color.BLUE,xEpochs.toDoubleArray(),yLoss.toDoubleArray())
    plot.setAxisLabel(0,"Epoch")
    val frame = JFrame("Softmax")
    frame.setSize(600, 600)
    frame.contentPane = plot
    frame.isVisible = true

}