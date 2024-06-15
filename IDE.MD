install plugins scala,planuml
for plan uml change path for "dot" 
and
chown root:users /usr/bin/dot
set executable!
touch ~/.bash_profile
echo "
export PATH=$PATH:/usr/bin/:/usr/share/graphviz/graphs
" >> ~/.bash_profile
cp ~/.bash_profile ~/.profile