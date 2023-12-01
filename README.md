# Pressbooks LinkChecker
This tool can check for broken links based on the following input:
- the URL to the page of a published Pressbooks resource
- pasted or typed HTML text

Note that this tool is customized to be used with Pressbooks - it will not crawl any other type of website. It can, however, be used to scan through provided HTML page code. Furthermore, it does not check text anchors or the internal link structure of Pressbooks, beyond the Table of Contents - the focus is on the links provided as part of the page content.

Any potential problems are displayed in a list that can be exported to a .csv file. Clicking on any row of the list will display a popup containing more detailed info about the error received.

