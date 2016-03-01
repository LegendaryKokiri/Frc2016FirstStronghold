package frclib;

import java.util.Comparator;
import java.util.Vector;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.*;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import hallib.HalDashboard;
import hallib.HalUtil;
import trclib.TrcDbgTrace;

public class FrcVision implements Runnable
{
    private static final String moduleName = "FrcVision";
    private static final boolean debugEnabled = false;
    private TrcDbgTrace dbgTrace = null;

    public interface ImageProvider
    {
        public boolean getImage(Image image);
    }   //interface ImageProvider

    private static final boolean visionPerfEnabled = false;

    public class ParticleReport implements Comparator<ParticleReport>, Comparable<ParticleReport>
    {
        public int imageWidth;
        public int imageHeight;
        public double percentAreaToImageArea;
        public double area;
        public double boundingRectLeft;
        public double boundingRectTop;
        public double boundingRectRight;
        public double boundingRectBottom;

        public int compareTo(ParticleReport r)
        {
            return (int)(r.area - this.area);
        }

        public int compare(ParticleReport r1, ParticleReport r2)
        {
            return (int)(r1.area - r2.area);
        }
    }   //class ParticleReport

    private ImageProvider camera;
    private ColorMode colorMode;
    private Range[] colorThresholds;
    private boolean doConvexHull;
    private ParticleFilterCriteria2[] filterCriteria;
    private ParticleFilterOptions2 filterOptions;

    private Image image;
    private Image binaryImage;
    private Object monitor;
    private Thread visionThread = null;

    private long processingInterval = 50;   // in msec
    private boolean sendImageEnabled = true;
    private boolean taskEnabled = false;
    private boolean oneShotEnabled = false;
    private Vector<ParticleReport> targets = null;

    public FrcVision(
            ImageProvider camera,
            ImageType imageType,
            ColorMode colorMode,
            Range[] colorThresholds,
            boolean doConvexHull,
            ParticleFilterCriteria2[] filterCriteria,
            ParticleFilterOptions2 filterOptions)
    {
        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(
                    moduleName,
                    false,
                    TrcDbgTrace.TraceLevel.API,
                    TrcDbgTrace.MsgLevel.INFO);
        }

        this.camera = camera;
        this.colorMode = colorMode;
        this.colorThresholds = colorThresholds;
        this.doConvexHull = doConvexHull;
        this.filterCriteria = filterCriteria;
        this.filterOptions = filterOptions;
        if (colorThresholds.length != 3)
        {
            throw new IllegalArgumentException(
                    "Color threshold array must have 3 elements.");
        }

        image = NIVision.imaqCreateImage(imageType, 0);
        binaryImage = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);

        monitor = new Object();
        visionThread = new Thread(this, "VisionTask");
        visionThread.start();
    }   //FrcVision

    public void setTaskEnabled(boolean enabled)
    {
        final String funcName = "setTaskEnabled";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(
                    funcName, TrcDbgTrace.TraceLevel.API,
                    "enabled=%s", Boolean.toString(enabled));
        }

        if (!taskEnabled && enabled)
        {
            //
            // Enable task.
            //
            synchronized(monitor)
            {
                taskEnabled = true;
                monitor.notify();
            }
        }
        else if (taskEnabled && !enabled)
        {
            //
            // Disable task.
            //
            taskEnabled = false;
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //setTaskEnabled

    public boolean isTaskEnabled()
    {
        final String funcName = "isTaskEnabled";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                    "=%s", Boolean.toString(taskEnabled));
        }

        return taskEnabled;
    }   //isTaskEnabled

    public void setProcessingInterval(long interval)
    {
        final String funcName = "setProcessingInterval";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API,
                    "interval=%dms", interval);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        processingInterval = interval;
    }   //setProcessInterval

    public long getProcessingInterval()
    {
        final String funcName = "getProcessingInterval";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                    "=%d", processingInterval);
        }

        return processingInterval;
    }   //getProcessingPeriod

    public Vector<ParticleReport> getTargets()
    {
        final String funcName = "getTargets";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(
                    funcName, TrcDbgTrace.TraceLevel.API);
        }

        Vector<ParticleReport> newTargets = null;
        synchronized(monitor)
        {
            if (!taskEnabled && targets == null)
            {
                oneShotEnabled = true;
                monitor.notify();
            }
            newTargets = targets;
            targets = null;
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(
                    funcName, TrcDbgTrace.TraceLevel.API);
        }
        return newTargets;
    }   //getTargets

    public void run()
    {
        while (true)
        {
            synchronized(monitor)
            {
                //
                // Wait until we are enabled.
                //
                while (!taskEnabled && !oneShotEnabled)
                {
                    try
                    {
                        monitor.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }

            long startTime = HalUtil.getCurrentTimeMillis();

            processImage();

            long sleepTime = processingInterval - (HalUtil.getCurrentTimeMillis() - startTime);
            HalUtil.sleep(sleepTime);
        }
    }   //run

    private void processImage()
    {
        final String funcName = "processImage";
        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.TASK);
        }

        double totalTime = 0.0;
        double startTime;
        double deltaTime;

        if (camera != null && camera.getImage(image))
        {
            if (visionPerfEnabled)
            {
                startTime = HalUtil.getCurrentTime();
            }
            NIVision.imaqColorThreshold(
                    binaryImage,
                    image,
                    255,
                    colorMode,
                    colorThresholds[0],
                    colorThresholds[1],
                    colorThresholds[2]);
            if (visionPerfEnabled)
            {
                deltaTime = HalUtil.getCurrentTime() - startTime;
                totalTime += deltaTime;
                SmartDashboard.putNumber("ColorThresholdTime", deltaTime);
            }

            if (doConvexHull)
            {
                if (visionPerfEnabled)
                {
                    startTime = HalUtil.getCurrentTime();
                }
                NIVision.imaqConvexHull(binaryImage, binaryImage, 1);
                if (visionPerfEnabled)
                {
                    deltaTime = HalUtil.getCurrentTime() - startTime;
                    totalTime += deltaTime;
                    SmartDashboard.putNumber("ConvexHullTime", deltaTime);
                }
            }

            if (visionPerfEnabled)
            {
                startTime = HalUtil.getCurrentTime();
            }
            NIVision.imaqParticleFilter4(
                    binaryImage,
                    binaryImage,
                    filterCriteria,
                    filterOptions,
                    null);
            if (visionPerfEnabled)
            {
                deltaTime = HalUtil.getCurrentTime() - startTime;
                totalTime += deltaTime;
                HalDashboard.putNumber("ParticleFilterTime", deltaTime);
            }

            int numParticles = NIVision.imaqCountParticles(binaryImage, 1);
            if(numParticles > 0)
            {
                //
                // Measure particles and sort by particle size.
                //
                Vector<ParticleReport> particles = new Vector<ParticleReport>();
                GetImageSizeResult imageSize =
                        NIVision.imaqGetImageSize(binaryImage);
                if (visionPerfEnabled)
                {
                    startTime = HalUtil.getCurrentTime();
                }

                for(int i = 0; i < numParticles; i++)
                {
                    ParticleReport par = new ParticleReport();
                    par.imageWidth = imageSize.width;
                    par.imageHeight = imageSize.height;
                    par.percentAreaToImageArea =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_AREA_BY_IMAGE_AREA);
                    par.area =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_AREA);
                    par.boundingRectTop =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_BOUNDING_RECT_TOP);
                    par.boundingRectLeft =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_BOUNDING_RECT_LEFT);
                    par.boundingRectBottom =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_BOUNDING_RECT_BOTTOM);
                    par.boundingRectRight =
                            NIVision.imaqMeasureParticle(
                                    binaryImage,
                                    i,
                                    0,
                                    MeasurementType.MT_BOUNDING_RECT_RIGHT);
                    particles.add(par);
                }
                particles.sort(null);
                if (visionPerfEnabled)
                {
                    deltaTime = HalUtil.getCurrentTime() - startTime;
                    totalTime += deltaTime;
                    HalDashboard.putNumber("PrepareReportTime", deltaTime);
                }

                if (sendImageEnabled)
                {
                    CameraServer.getInstance().setImage(binaryImage);
                }

                synchronized(monitor)
                {
                    oneShotEnabled = false;
                    targets = particles;
                    particles = null;
                }
            }
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.TASK);
        }
    }   //processImage

}   //class FrcVision
