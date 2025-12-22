//Sfm mapping

docker build -t my_openmvg .

docker run -it -v "C:\Users\holym\desktop\vecturai_demo:/data" my_openmvg /bin/bash

mkdir frames
mkdir matches
mkdir reconstruction

upload your video as "video.mp4" to the main directory

ffmpeg -i video.mp4 -vf fps=4 frames/frame_%04d.jpg

openMVG_main_SfMInit_ImageListing -i frames/ -o matches/ -f 1600

openMVG_main_ComputeFeatures -i matches/sfm_data.json -o matches/

openMVG_main_PairGenerator -i matches/sfm_data.json -o matches/pairs.bin -m CONTIGUOUS -c 20

openMVG_main_ComputeMatches -i matches/sfm_data.json -p matches/pairs.bin -o matches/matches.putative.bin

openMVG_main_GeometricFilter -i matches/sfm_data.json -m matches/matches.putative.bin -o matches/matches.f.bin

openMVG_main_SfM --sfm_engine GLOBAL -i matches/sfm_data.json -m matches/ -o reconstruction/

//Python 

python -m pip install open3d numpy matplotlib scipy

python navigation_final.py


