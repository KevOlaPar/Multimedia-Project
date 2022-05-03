import os
import matplotlib.pyplot as plt
import numpy as np
import cv2 as cv
from google.cloud import vision
import json
import sys
import argparse
from difflib import SequenceMatcher


## logos that can be detected by this program
logos = ["Starbucks", "Subway", "McDonald's", "NFL", "American Eagle Outfitters", "Hard Rock Cafe"]

class Frame:

    def __init__(self, rgb):
        self.rgb = rgb

class VideoReader:

    def __init__(self, path):
        self.path = path
        self.w = 480
        self.h = 270
        self.sz = 480*270
        self.totalFrames = os.path.getsize(self.path)//(self.sz*3);
        self.seconds = 6
        # self.seconds = self.totalFrames//30
        self.file = open(path, "rb")

    def getFrame(self, frameNo):
        self.file.seek(frameNo * 3 * self.sz)
        bytes = self.file.read(3 * self.sz)
        rgb = np.zeros((self.h, self.w, 3), dtype=np.uint8)

        pix = 0
        for r in range(self.h):
            for c in range(self.w):
                rgb[r][c][0] = bytes[pix]
                rgb[r][c][1] = bytes[pix + self.sz]
                rgb[r][c][2] = bytes[pix + self.sz * 2]
                pix+=1
                
        return Frame(rgb)

    
    def showFrame(self, frameNo):
        plt.imshow(self.getFrame(frameNo).rgb)
        plt.show()

class Result:

    def __init__(self, score, boundry, frameNo):
        self.score = score
        self.boundry = boundry
        self.frameNo = frameNo

    @classmethod
    def fromGoogle(cls, frameNo, logo):
        return cls(logo.score, Boundry.fromGoogle(logo.bounding_poly), frameNo)

    def to_dict(r):
        result = dict()
        result["score"] = r.score
        result["boundry"] = r.boundry.to_dict()
        result["frameNo"] = r.frameNo
        return result

    @classmethod
    def fromDict(cls, d):
        return cls(d["score"], Boundry.fromDict(d["boundry"]), d["frameNo"])

class Boundry:
    def __init__(self, tl, tr, bl, br):
        self.tl = tl
        self.tr = tr
        self.bl = bl
        self.br = br

    @classmethod
    def fromDict(cls, d):
        tl = (d["tl"][0], d["tl"][1])
        tr = (d["tr"][0], d["tr"][1])
        bl = (d["bl"][0], d["bl"][1])
        br = (d["br"][0], d["br"][1])
        return cls(tl, tr, bl, br)

    @classmethod
    def fromGoogle(cls, boundry):
        tl = (boundry.vertices[0].x, boundry.vertices[0].y)
        tr = (boundry.vertices[1].x, boundry.vertices[1].y)
        br = (boundry.vertices[2].x, boundry.vertices[2].y)
        bl = (boundry.vertices[3].x, boundry.vertices[3].y)
        return cls(tl, tr, bl, br)

    def to_dict(b):
        result = dict()
        result["tl"] = list(b.tl)
        result["tr"] = list(b.tr)
        result["bl"] = list(b.bl)
        result["br"] = list(b.br)
        return result

    def to_final_dict(b):
        result = [list(b.tl), list(b.br)]
        return result

class Detector:

    def __init__(self, videoPath, logos):
        self.reader = VideoReader(videoPath)
        self.client = vision.ImageAnnotatorClient()
        self.logos = logos
        self.results = dict()
        self.STEPS = 5
        self.frames = dict()
        for logo in logos:
            self.results[logo] = []

    def parseLogos(self, frameNo, logos):
        if len(logos) > 0 and logos[0].description in self.logos:
            name = logos[0].description
            print(name)
            self.results[name].append(Result.fromGoogle(frameNo, logos[0]))
            

    def detectLogoInFrame(self, frameNo):
        image = self.reader.getFrame(frameNo).rgb
        content = cv.imencode('.jpg', image)[1].tobytes()

        content = vision.Image(content=content)

        response = self.client.logo_detection(image=content)
        logos = response.logo_annotations

        if response.error.message:
            raise Exception(
                '{}\nFor more info on error messages, check: '
                'https://cloud.google.com/apis/design/errors'.format(
                    response.error.message))

        self.parseLogos(frameNo, logos)

        return logos

    def findNames(self):

        names = set()
        for frameNo in range(0,9000, 50):
            print("finding names in ", frameNo)
            image = self.reader.getFrame(frameNo).rgb
            content = cv.imencode('.jpg', image)[1].tobytes()

            content = vision.Image(content=content)

            response = self.client.logo_detection(image=content)
            logos = response.logo_annotations

            if response.error.message:
                raise Exception(
                    '{}\nFor more info on error messages, check: '
                    'https://cloud.google.com/apis/design/errors'.format(
                        response.error.message))

            if len(logos) > 0:
                for logo in logos:
                    names.add(logo.description)
        
        print(names)

    def detectInBetweenFrames(self, startFrame, endFrame):
        for frame in range(startFrame+self.STEPS, endFrame, self.STEPS):
            print("Detection in frame ", frame)
            self.detectLogoInFrame(frame)


    def intersect(self, intervals):
        intervals.sort()
        for i in range(1, len(intervals)):
            if intervals[i][1] <= intervals[i- 1][1]:
                return True
        return False

    def detect(self):
        # self.detectLogoInFrame(180*30)
        # print(self.toJson())

        for frame in range(0, 9000, 30):
            print("Detection in frame ", frame)
            self.detectLogoInFrame(frame)

        intervals = []
        for logo, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)
            start = results[0].frameNo
            end = results[-1].frameNo
            intervals.append((start, end))

        if(self.intersect(intervals)):
            sys.exit("FATAL ERROR: logos are intersecting")
        
        self.results = dict()
        for logo in self.logos:
            self.results[logo] = []

        for interval in intervals:
            self.detectInBetweenFrames(interval[0]-15, interval[1] + 15)

        self.interpolate()

    def interpolate(self):
        for _, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)
        
        for _, results in self.results.items():
            for i in range(len(results)-1):
                if results[i+1].frameNo - results[i].frameNo <= self.STEPS:
                    self.interpolateUtil(results, results[i+1], results[i])
        
        for _, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)

    def interpolateUtil(self, r, e, s):
        framesToInsert = e.frameNo - s.frameNo - 1
        if framesToInsert <= 0:
            return
        bs = s.boundry
        be = e.boundry
        tls = self.interpolateValues(bs.tl, be.tl, framesToInsert)
        trs = self.interpolateValues(bs.tr, be.tr, framesToInsert)
        bls = self.interpolateValues(bs.bl, be.bl, framesToInsert)
        brs = self.interpolateValues(bs.br, be.br, framesToInsert)
        for i in range(framesToInsert):
            r.append(Result(s.score, Boundry(tls[i], trs[i], bls[i], brs[i]), s.frameNo + i + 1))
        
    def interpolateValues(self, s, e, values):
        inter = values + 1
        changex = (e[0]-s[0])/inter
        changey = (e[1]-s[1])/inter
        v = []
        x = s[0]
        y = s[1]
        for _ in range(values):
            x+=changex
            y+=changey
            v.append((int(x), int(y))) 
        return v


    def toJson(self):
        d = dict()
        for logo in self.results:
            d[logo] = []
            for r in  self.results[logo]:
                d[logo].append(r.to_dict())
        return json.dumps(d)

    def loadJson(self, j):
        d = json.loads(j)
        for logo in d:
            self.results[logo] = []
            for result in d[logo]:
                self.results[logo].append(Result.fromDict(result))

    def toFinalJson(self):
        fj = dict()

        # Delete empty lists
        for logo in list(self.results.keys()):
            if len(self.results[logo]) == 0:
                del self.results[logo]

        for logo, results in self.results.items():
            results.sort(key = lambda x: x.frameNo)

        fj["logos"] = []
        
        for logo, results in self.results.items():
            startFrame = results[0].frameNo
            fj["logos"].append({logo: startFrame})
        
        d = dict()

        for logo, results in self.results.items():
            for r in results:
                if r.frameNo not in d:
                    d[r.frameNo] = r.boundry.to_final_dict()
        
        d = dict(sorted(d.items()))
        fj["frames"] = d

        return json.dumps(fj)

    def fillFrames(self):
        for results in self.results.values():
            for r in results:
                self.frames[r.frameNo] = r.boundry

    def getFrame(self, frameNo):
        frame = self.reader.getFrame(frameNo).rgb
        if frameNo in self.frames:
            b = self.frames[frameNo]
            cv.rectangle(frame, b.tl, b.bl, (0,255,0), 2)            
        return frame

    def play_video(self):
        # window name and size
        self.fillFrames()
        cv.namedWindow("video", cv.WINDOW_AUTOSIZE)
        i=0
        while i < 9000:
            # Read video capture
            frame = self.getFrame(i)
            frame = cv.cvtColor(frame,cv.COLOR_RGB2BGR)
            # img = im.fromarray(frame)
            # Display each frame
            cv.imshow("video", frame)
            # show one frame at a time
            key = cv.waitKey(00)
            # Quit when 'q' is pressed
            if key == ord('q'):
                break
            i+=1
        # Exit and distroy all windows
        cv.destroyAllWindows()

def similar(b):
    dist = []
    for logo in logos:
        dist.append(SequenceMatcher(None, logo.lower(), b.lower()).ratio()) 
    mi = 0
    for i, d in enumerate(dist):
        if d > dist[mi]:
            mi = i
    return logos[mi]

parser = argparse.ArgumentParser(description='Detect Logos Boundries')
parser.add_argument('--inputfile', '-i', help='Name of the video file', required=True)
parser.add_argument('--logos', '-l', help='Name of logos to be detected', nargs="+", required=True)

args = parser.parse_args()

l = []
for logo in args.logos:
    l.append(similar(logo))
args.inputfile = args.inputfile.replace("\s", " ")
detector = Detector(args.inputfile, l)

detector.detect()
# print(detector.findNames())

# # detector.findNames()
with open("d1_temp", "w") as text_file:
    text_file.write(detector.toJson())

with open("detector.json", "w") as text_file:
    text_file.write(detector.toFinalJson())









