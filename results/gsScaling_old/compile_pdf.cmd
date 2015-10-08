@echo off
for /r %%i in (*.gp) do (
    gnuplot %%~ni.gp
    ps2pdf -dPDFSETTINGS#/prepress -dEmbedAllFonts#true -dUseFlateCompression#true %%~ni.eps
    pdfcrop %%~ni.eps.pdf
    del %%~ni.eps
    del %%~ni.eps.pdf
    del %%~ni.pdf
    ren %%~ni.eps-crop.pdf %%~ni.pdf
)